import { useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import FilterBuilder from '../components/FilterBuilder';
import { apiErrorMessage, listFieldDefinitions, listItems, type Item } from '../items/api';
import { filterFields, toFilterParams, type FilterRow } from '../items/filters';
import { timeAgo } from '../items/timeAgo';
import { useProjects } from '../projects/ProjectContext';

const PAGE_SIZE = 25;
const cell = { padding: '0.4rem 0.5rem', textAlign: 'left' as const, borderBottom: '1px solid #e0e0e0' };

interface Column {
  label: string;
  sortField?: string;
  render: (item: Item) => React.ReactNode;
}

const COLUMNS: Column[] = [
  { label: 'Key', sortField: 'key', render: (item) => <Link to={`/items/${item.key}`}>{item.key}</Link> },
  { label: 'Title', sortField: 'title', render: (item) => item.title },
  { label: 'Type', render: (item) => item.type },
  { label: 'Status', sortField: 'status', render: (item) => item.statusName },
  { label: 'Assignee', sortField: 'assignee', render: (item) => item.assignee ?? '-' },
  { label: 'Priority', sortField: 'priority', render: (item) => item.priority },
  { label: 'Updated', sortField: 'updatedAt', render: (item) => timeAgo(item.updatedAt) },
];

function ProjectItems({ projectKey }: { projectKey: string }) {
  const [appliedRows, setAppliedRows] = useState<FilterRow[]>([]);
  const [sortField, setSortField] = useState('updatedAt');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);

  const { data: definitions = [] } = useQuery({
    queryKey: ['fieldDefinitions', projectKey],
    queryFn: () => listFieldDefinitions(projectKey),
  });
  const filters = toFilterParams(appliedRows);
  const sort = `${sortField},${sortDirection}`;
  const { data, error } = useQuery({
    queryKey: ['items', projectKey, filters, sort, page],
    queryFn: () => listItems({ project: projectKey, filters, sort, page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  });

  const applyFilters = (rows: FilterRow[]) => {
    setAppliedRows(rows);
    setPage(0);
  };
  const sortBy = (field: string) => {
    setSortDirection(field === sortField && sortDirection === 'asc' ? 'desc' : 'asc');
    setSortField(field);
    setPage(0);
  };

  return (
    <>
      <h1 style={{ fontSize: '1.4rem', marginTop: 0 }}>Items · {projectKey}</h1>
      <FilterBuilder fields={filterFields(definitions)} onApply={applyFilters} />
      {error && <p role="alert">{apiErrorMessage(error)}</p>}
      <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff' }}>
        <thead>
          <tr>
            {COLUMNS.map((column) => (
              <th key={column.label} style={cell}>
                {column.sortField ? (
                  <button onClick={() => sortBy(column.sortField!)}>
                    {column.label}
                    {sortField === column.sortField && (sortDirection === 'asc' ? ' ^' : ' v')}
                  </button>
                ) : (
                  column.label
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data?.content.map((item) => (
            <tr key={item.key}>
              {COLUMNS.map((column) => (
                <td key={column.label} style={cell}>
                  {column.render(item)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {data?.content.length === 0 && <p>No items match.</p>}
      {data && data.totalPages > 1 && (
        <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </button>
          <span>
            Page {page + 1} of {data.totalPages}
          </span>
          <button disabled={data.last} onClick={() => setPage(page + 1)}>
            Next
          </button>
        </div>
      )}
    </>
  );
}

export default function ItemsPage() {
  const { currentProject } = useProjects();
  if (!currentProject) {
    return <p>There are no projects yet.</p>;
  }
  return <ProjectItems key={currentProject.key} projectKey={currentProject.key} />;
}

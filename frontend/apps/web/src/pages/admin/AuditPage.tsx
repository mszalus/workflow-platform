import { useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listAuditEntries } from '../../admin/api';
import { apiErrorMessage } from '../../items/api';

const PAGE_SIZE = 25;
const cell = { padding: '0.4rem 0.5rem', textAlign: 'left' as const, borderBottom: '1px solid #e0e0e0' };

interface AuditFilters {
  who: string;
  what: string;
  from: string;
  to: string;
}

const NO_FILTERS: AuditFilters = { who: '', what: '', from: '', to: '' };

export default function AuditPage() {
  const [draft, setDraft] = useState(NO_FILTERS);
  const [applied, setApplied] = useState(NO_FILTERS);
  const [page, setPage] = useState(0);

  const { data, error } = useQuery({
    queryKey: ['audit', applied, page],
    queryFn: () =>
      listAuditEntries({
        userId: applied.who.trim(),
        eventType: applied.what.trim(),
        from: applied.from && `${applied.from}T00:00:00Z`,
        to: applied.to && `${applied.to}T23:59:59Z`,
        page,
        size: PAGE_SIZE,
      }),
    placeholderData: keepPreviousData,
  });
  const setFilter = (name: keyof AuditFilters, value: string) => setDraft({ ...draft, [name]: value });
  const apply = () => {
    setApplied(draft);
    setPage(0);
  };

  return (
    <>
      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap' }}>
        <h1 style={{ fontSize: '1.4rem', margin: 0, flex: 1 }}>Audit log</h1>
        <input
          aria-label="Who"
          placeholder="Who"
          value={draft.who}
          onChange={(e) => setFilter('who', e.target.value)}
        />
        <input
          aria-label="What"
          placeholder="What, e.g. item.created"
          value={draft.what}
          onChange={(e) => setFilter('what', e.target.value)}
        />
        <input aria-label="From" type="date" value={draft.from} onChange={(e) => setFilter('from', e.target.value)} />
        <input aria-label="To" type="date" value={draft.to} onChange={(e) => setFilter('to', e.target.value)} />
        <button onClick={apply}>Apply</button>
      </div>
      {error && <p role="alert">{apiErrorMessage(error)}</p>}
      <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff' }}>
        <thead>
          <tr>
            {['When', 'Who', 'What', 'On', 'Details'].map((heading) => (
              <th key={heading} style={cell}>
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data?.content.map((entry) => (
            <tr key={entry.id}>
              <td style={cell}>{new Date(entry.timestamp).toLocaleString()}</td>
              <td style={cell}>{entry.userId}</td>
              <td style={cell}>{entry.eventType}</td>
              <td style={cell}>{entry.entityId}</td>
              <td style={{ ...cell, fontSize: '0.8rem', wordBreak: 'break-all' }}>{JSON.stringify(entry.details)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {data?.content.length === 0 && <p>No audit entries match.</p>}
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

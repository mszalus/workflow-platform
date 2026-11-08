import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listItems, type Item, type StatusCategory } from '../items/api';
import { timeAgo } from '../items/timeAgo';

const MAX_ITEMS = 100;
const RECENTLY_REPORTED = 5;
const DONE_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;

const GROUPS: { category: StatusCategory; title: string }[] = [
  { category: 'IN_PROGRESS', title: 'IN PROGRESS' },
  { category: 'TODO', title: 'TO DO' },
  { category: 'OPEN', title: 'OPEN' },
  { category: 'DONE', title: 'DONE, last 7 days' },
];

function isDoneTooLongAgo(item: Item) {
  return item.statusCategory === 'DONE' && Date.now() - new Date(item.updatedAt).getTime() > DONE_WINDOW_MS;
}

function ItemLine({ item }: { item: Item }) {
  return (
    <div style={{ display: 'flex', gap: '1rem', padding: '0.25rem 0' }}>
      <Link to={`/items/${item.key}`} style={{ width: 90 }}>
        {item.key}
      </Link>
      <span style={{ flex: 1 }}>{item.title}</span>
      <span style={{ width: 80 }}>{item.type}</span>
      <span style={{ width: 110 }}>{item.statusName}</span>
      <span style={{ width: 70 }}>{item.priority}</span>
      <span style={{ width: 90 }}>{timeAgo(item.updatedAt)}</span>
    </div>
  );
}

export default function MyWorkPage() {
  const { data: assigned } = useQuery({
    queryKey: ['items', 'assigned-to-me'],
    queryFn: () => listItems({ filters: ['assignee:eq:me'], size: MAX_ITEMS }),
  });
  const { data: reported } = useQuery({
    queryKey: ['items', 'reported-by-me'],
    queryFn: () => listItems({ filters: ['reporter:eq:me'], size: RECENTLY_REPORTED }),
  });
  const myItems = (assigned?.content ?? []).filter((item) => !isDoneTooLongAgo(item));

  return (
    <>
      <h1 style={{ fontSize: '1.4rem', marginTop: 0 }}>My work</h1>
      {myItems.length === 0 && <p>Nothing is assigned to you.</p>}
      {GROUPS.map(({ category, title }) => {
        const items = myItems.filter((item) => item.statusCategory === category);
        return (
          items.length > 0 && (
            <section key={category} style={{ marginBottom: '1rem' }}>
              <h2 style={{ fontSize: '0.9rem', marginBottom: '0.25rem' }}>
                {title} ({items.length})
              </h2>
              {items.map((item) => (
                <ItemLine key={item.key} item={item} />
              ))}
            </section>
          )
        );
      })}
      {reported && reported.content.length > 0 && (
        <section>
          <h2 style={{ fontSize: '0.9rem', marginBottom: '0.25rem' }}>Recently updated items I reported</h2>
          {reported.content.map((item) => (
            <ItemLine key={item.key} item={item} />
          ))}
        </section>
      )}
    </>
  );
}

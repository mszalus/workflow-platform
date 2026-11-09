import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import FieldInput from '../components/FieldInput';
import ItemDiagram from '../components/ItemDiagram';
import {
  addComment,
  apiErrorMessage,
  completeStep,
  getItem,
  listComments,
  listHistory,
  listFieldDefinitions,
  transitionItem,
  updateItem,
  type FieldValues,
  type Item,
  type ItemHistoryEntry,
  type Priority,
} from '../items/api';
import { PRIORITIES } from '../items/filters';
import { timeAgo } from '../items/timeAgo';

const panel = { background: '#fff', padding: '1rem', borderRadius: 4, marginBottom: '1rem' };

function useItemSaver(itemKey: string) {
  const queryClient = useQueryClient();
  return (item: Item) => {
    queryClient.setQueryData(['item', itemKey], item);
    queryClient.invalidateQueries({ queryKey: ['history', itemKey] });
    queryClient.invalidateQueries({ queryKey: ['items'] });
  };
}

function WorkflowActions({ item }: { item: Item }) {
  const saveItem = useItemSaver(item.key);
  const move = useMutation({
    mutationFn: (transitionId: string) => transitionItem(item.key, transitionId),
    onSuccess: saveItem,
  });
  const finishStep = useMutation({
    mutationFn: (stepId: string) => completeStep(item.key, stepId),
    onSuccess: saveItem,
  });
  const failure = move.error ?? finishStep.error;

  return (
    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap' }}>
      <strong style={{ background: '#e3f2fd', padding: '0.2rem 0.6rem', borderRadius: 4 }}>{item.statusName}</strong>
      {item.transitions?.map((transition) => (
        <button key={transition.id} disabled={move.isPending} onClick={() => move.mutate(transition.id)}>
          {transition.name}
        </button>
      ))}
      {item.openSteps?.map((step) => (
        <button key={step.id} disabled={finishStep.isPending} onClick={() => finishStep.mutate(step.id)}>
          Complete: {step.name}
        </button>
      ))}
      {failure && <span role="alert">{apiErrorMessage(failure)}</span>}
    </div>
  );
}

function Description({ item }: { item: Item }) {
  const saveItem = useItemSaver(item.key);
  const [editing, setEditing] = useState(false);
  const [text, setText] = useState(item.description ?? '');
  const save = useMutation({
    mutationFn: () => updateItem(item.key, { description: text }),
    onSuccess: (updated) => {
      saveItem(updated);
      setEditing(false);
    },
  });

  return (
    <section style={panel}>
      <h2 style={{ fontSize: '1rem', marginTop: 0 }}>Description</h2>
      {editing ? (
        <>
          <textarea
            aria-label="Description"
            value={text}
            onChange={(event) => setText(event.target.value)}
            style={{ width: '100%', minHeight: 100 }}
          />
          <button onClick={() => save.mutate()}>Save description</button>{' '}
          <button onClick={() => setEditing(false)}>Cancel</button>
          {save.isError && <p role="alert">{apiErrorMessage(save.error)}</p>}
        </>
      ) : (
        <>
          <p style={{ whiteSpace: 'pre-wrap' }}>{item.description || 'No description.'}</p>
          <button onClick={() => setEditing(true)}>Edit</button>
        </>
      )}
    </section>
  );
}

function Comments({ itemKey }: { itemKey: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const { data: comments = [] } = useQuery({
    queryKey: ['comments', itemKey],
    queryFn: () => listComments(itemKey),
  });
  const post = useMutation({
    mutationFn: () => addComment(itemKey, text),
    onSuccess: () => {
      setText('');
      queryClient.invalidateQueries({ queryKey: ['comments', itemKey] });
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    post.mutate();
  };

  return (
    <section style={panel}>
      <h2 style={{ fontSize: '1rem', marginTop: 0 }}>Comments</h2>
      {comments.map((comment) => (
        <div key={comment.id} style={{ borderBottom: '1px solid #eee', padding: '0.5rem 0' }}>
          <div style={{ fontSize: '0.85rem', color: '#666' }}>
            {comment.userId} · {timeAgo(comment.createdAt)}
          </div>
          <div style={{ whiteSpace: 'pre-wrap' }}>{comment.content}</div>
        </div>
      ))}
      <form onSubmit={submit} style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
        <input
          aria-label="Add a comment"
          placeholder="Add a comment..."
          required
          value={text}
          onChange={(event) => setText(event.target.value)}
          style={{ flex: 1 }}
        />
        <button type="submit" disabled={post.isPending}>
          Post
        </button>
      </form>
      {post.isError && <p role="alert">{apiErrorMessage(post.error)}</p>}
    </section>
  );
}

const LONG_TEXT_FIELDS = ['description'];

function describeChange(entry: ItemHistoryEntry) {
  if (entry.kind === 'CREATED') return 'created';
  if (entry.kind === 'STATUS') {
    const transition = entry.transition ? ` (${entry.transition})` : '';
    const reason = entry.reason ? ` - ${entry.reason}` : '';
    return `${entry.from} -> ${entry.to}${transition}${reason}`;
  }
  const field = entry.field?.replace(/^fields\./, '') ?? '';
  if (LONG_TEXT_FIELDS.includes(field)) return `${field} changed`;
  return `${field} ${entry.from ?? 'empty'} -> ${entry.to ?? 'empty'}`;
}

function History({ itemKey }: { itemKey: string }) {
  const { data: entries = [] } = useQuery({ queryKey: ['history', itemKey], queryFn: () => listHistory(itemKey) });

  return (
    <section style={panel}>
      <h2 style={{ fontSize: '1rem', marginTop: 0 }}>History</h2>
      {entries.map((entry, index) => (
        <div key={index} style={{ display: 'flex', gap: '0.75rem', padding: '0.2rem 0', fontSize: '0.9rem' }}>
          <span style={{ width: 70, color: '#666' }}>{timeAgo(entry.timestamp)}</span>
          <span style={{ width: 90 }}>{entry.actor}</span>
          <span style={{ flex: 1 }}>{describeChange(entry)}</span>
        </div>
      ))}
    </section>
  );
}

function Details({ item }: { item: Item }) {
  const saveItem = useItemSaver(item.key);
  const [assignee, setAssignee] = useState(item.assignee ?? '');
  const [priority, setPriority] = useState(item.priority);
  const [fields, setFields] = useState<FieldValues>(item.fields);
  const { data: definitions = [] } = useQuery({
    queryKey: ['fieldDefinitions', item.project, item.type],
    queryFn: () => listFieldDefinitions(item.project, item.type),
  });
  const save = useMutation({
    mutationFn: () => updateItem(item.key, { assignee: assignee.trim(), priority, fields }),
    onSuccess: saveItem,
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };

  return (
    <form onSubmit={submit} style={panel}>
      <label style={{ display: 'block', marginBottom: '0.5rem' }}>
        <div style={{ fontSize: '0.85rem' }}>Assignee</div>
        <input value={assignee} onChange={(event) => setAssignee(event.target.value)} style={{ width: '100%' }} />
      </label>
      <label style={{ display: 'block', marginBottom: '0.5rem' }}>
        <div style={{ fontSize: '0.85rem' }}>Priority</div>
        <select
          value={priority}
          onChange={(event) => setPriority(event.target.value as Priority)}
          style={{ width: '100%' }}
        >
          {PRIORITIES.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
      <dl style={{ fontSize: '0.85rem', margin: '0.5rem 0' }}>
        <dt>Reporter</dt>
        <dd style={{ margin: 0 }}>{item.reporter}</dd>
        <dt>Created</dt>
        <dd style={{ margin: 0 }}>{item.createdAt.slice(0, 10)}</dd>
        <dt>Updated</dt>
        <dd style={{ margin: 0 }}>{timeAgo(item.updatedAt)}</dd>
      </dl>
      {definitions.map((definition) => (
        <FieldInput
          key={definition.id}
          definition={definition}
          value={fields[definition.fieldKey]}
          onChange={(value) => setFields({ ...fields, [definition.fieldKey]: value })}
        />
      ))}
      <button type="submit" disabled={save.isPending}>
        Save changes
      </button>
      {save.isError && <p role="alert">{apiErrorMessage(save.error)}</p>}
    </form>
  );
}

export default function ItemDetailPage() {
  const { key = '' } = useParams();
  const { data: item, error } = useQuery({ queryKey: ['item', key], queryFn: () => getItem(key) });

  if (error) return <p role="alert">{apiErrorMessage(error)}</p>;
  if (!item) return <p>Loading...</p>;

  return (
    <>
      <div style={{ color: '#666' }}>
        {item.key} · {item.type}
      </div>
      <h1 style={{ fontSize: '1.4rem', margin: '0.25rem 0 0.75rem' }}>{item.title}</h1>
      <WorkflowActions item={item} />
      <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
        <div style={{ flex: 2 }}>
          <Description item={item} />
          <Comments itemKey={item.key} />
          <History itemKey={item.key} />
        </div>
        <div style={{ flex: 1 }}>
          <Details key={item.updatedAt} item={item} />
          <ItemDiagram versionId={item.workflowVersionId} statusId={item.statusId} />
        </div>
      </div>
    </>
  );
}

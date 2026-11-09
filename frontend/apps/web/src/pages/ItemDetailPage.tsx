import { useState, type ChangeEvent, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { useIsAdmin } from '../auth/useIsAdmin';
import FieldInput from '../components/FieldInput';
import ItemDiagram from '../components/ItemDiagram';
import {
  addComment,
  apiErrorMessage,
  completeStep,
  deleteAttachment,
  downloadAttachment,
  getItem,
  listAttachments,
  listComments,
  listHistory,
  listFieldDefinitions,
  transitionItem,
  updateItem,
  uploadAttachment,
  type FieldValues,
  type Item,
  type ItemAttachment,
  type ItemHistoryEntry,
  type Priority,
} from '../items/api';
import { PRIORITIES } from '../items/filters';
import { timeAgo } from '../items/timeAgo';
import { useAuth } from '../shared-ui';

const panel = { background: '#fff', padding: '1rem', borderRadius: 4, marginBottom: '1rem' };
const MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024;

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

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function Attachments({ itemKey }: { itemKey: string }) {
  const queryClient = useQueryClient();
  const { username } = useAuth();
  const isAdmin = useIsAdmin();
  const [tooLarge, setTooLarge] = useState(false);
  const { data: attachments = [] } = useQuery({
    queryKey: ['attachments', itemKey],
    queryFn: () => listAttachments(itemKey),
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['attachments', itemKey] });
  const upload = useMutation({ mutationFn: (file: File) => uploadAttachment(itemKey, file), onSuccess: refresh });
  const remove = useMutation({ mutationFn: (id: string) => deleteAttachment(itemKey, id), onSuccess: refresh });
  const download = useMutation({ mutationFn: (file: ItemAttachment) => downloadAttachment(itemKey, file) });
  const failure = upload.error ?? remove.error ?? download.error;

  const chooseFile = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    const fits = file.size <= MAX_ATTACHMENT_BYTES;
    setTooLarge(!fits);
    if (fits) upload.mutate(file);
  };
  const confirmDelete = (file: ItemAttachment) => {
    if (window.confirm(`Delete ${file.fileName}?`)) remove.mutate(file.id);
  };

  return (
    <section style={panel}>
      <h2 style={{ fontSize: '1rem', marginTop: 0 }}>Attachments</h2>
      {attachments.map((file) => (
        <div key={file.id} style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', padding: '0.2rem 0' }}>
          <button onClick={() => download.mutate(file)}>{file.fileName}</button>
          <span style={{ flex: 1, fontSize: '0.85rem', color: '#666' }}>
            {formatSize(file.sizeBytes)} · {file.uploadedBy} · {timeAgo(file.uploadedAt)}
          </span>
          {(isAdmin || file.uploadedBy === username) && (
            <button aria-label={`Delete ${file.fileName}`} onClick={() => confirmDelete(file)}>
              Delete
            </button>
          )}
        </div>
      ))}
      <label style={{ display: 'block', marginTop: '0.5rem', fontSize: '0.85rem' }}>
        Attach a file (up to 10 MB) <input type="file" disabled={upload.isPending} onChange={chooseFile} />
      </label>
      {tooLarge && <p role="alert">Files can be at most 10 MB</p>}
      {failure && <p role="alert">{apiErrorMessage(failure)}</p>}
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
          <Attachments itemKey={item.key} />
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

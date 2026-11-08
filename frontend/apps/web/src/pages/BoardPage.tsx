import { useState, type DragEvent } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  apiErrorMessage,
  getItem,
  listItems,
  transitionItem,
  type Item,
  type StatusCategory,
  type Transition,
} from '../items/api';
import { STATUS_CATEGORIES } from '../items/filters';
import { useProjects, type Project } from '../projects/ProjectContext';

const MAX_ITEMS = 500;
const ALL = '';
const CLOSED_STATUS_NAME = 'Closed';

interface MoveChoice {
  transition: Transition;
  targetNames: string[];
}

interface PendingMove {
  item: Item;
  categoryLabel: string;
  choices: MoveChoice[];
}

function categoryLabel(category: StatusCategory) {
  return STATUS_CATEGORIES.find((option) => option.value === category)?.label ?? category;
}

function targetsIn(item: Item, transition: Transition, category: StatusCategory) {
  return transition.targetStatusIds
    .map((id) => item.statuses?.find((status) => status.id === id))
    .filter((status) => (status?.category ?? 'DONE') === category)
    .map((status) => status?.name ?? CLOSED_STATUS_NAME);
}

function movesInto(item: Item, category: StatusCategory): MoveChoice[] {
  return (item.transitions ?? [])
    .map((transition) => ({ transition, targetNames: targetsIn(item, transition, category) }))
    .filter((choice) => choice.targetNames.length > 0);
}

function Card({ item }: { item: Item }) {
  return (
    <div
      draggable
      data-testid={`card-${item.key}`}
      onDragStart={(event) => event.dataTransfer.setData('text/plain', item.key)}
      style={{ background: '#fff', borderRadius: 4, padding: '0.5rem', marginBottom: '0.5rem', cursor: 'grab' }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
        <Link to={`/items/${item.key}`}>{item.key}</Link>
        <span>{item.type}</span>
      </div>
      <div style={{ margin: '0.25rem 0' }}>{item.title}</div>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
        <span style={{ background: '#e3f2fd', padding: '0 0.4rem', borderRadius: 4 }}>{item.statusName}</span>
        <span>{item.assignee ?? '--'}</span>
      </div>
    </div>
  );
}

function MovePicker({
  move,
  onCancel,
  onConfirm,
}: {
  move: PendingMove;
  onCancel: () => void;
  onConfirm: (transitionId: string) => void;
}) {
  const [selected, setSelected] = useState(move.choices[0].transition.id);

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.4)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 20,
      }}
    >
      <div
        role="dialog"
        aria-label="Choose a transition"
        style={{ background: '#fff', padding: '1.5rem', borderRadius: 4 }}
      >
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>
          Move {move.item.key} to {move.categoryLabel}
        </h2>
        {move.choices.map(({ transition, targetNames }) => (
          <label key={transition.id} style={{ display: 'block', marginBottom: '0.5rem' }}>
            <input
              type="radio"
              name="transition"
              checked={selected === transition.id}
              onChange={() => setSelected(transition.id)}
            />{' '}
            {transition.name} -&gt; {targetNames.join(', ')}
          </label>
        ))}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
          <button onClick={onCancel}>Cancel</button>
          <button onClick={() => onConfirm(selected)}>Move</button>
        </div>
      </div>
    </div>
  );
}

function ProjectBoard({ project }: { project: Project }) {
  const queryClient = useQueryClient();
  const [typeFilter, setTypeFilter] = useState(ALL);
  const [assigneeFilter, setAssigneeFilter] = useState(ALL);
  const [message, setMessage] = useState<string>();
  const [pendingMove, setPendingMove] = useState<PendingMove>();

  const { data } = useQuery({
    queryKey: ['items', 'board', project.key],
    queryFn: () => listItems({ project: project.key, size: MAX_ITEMS }),
  });
  const items = data?.content ?? [];
  const assignees = [...new Set(items.flatMap((item) => item.assignee ?? []))].sort();
  const visibleItems = items.filter(
    (item) =>
      (typeFilter === ALL || item.type === typeFilter) && (assigneeFilter === ALL || item.assignee === assigneeFilter),
  );

  const runTransition = async (key: string, transitionId: string) => {
    setPendingMove(undefined);
    try {
      await transitionItem(key, transitionId);
      await queryClient.invalidateQueries({ queryKey: ['items'] });
      queryClient.removeQueries({ queryKey: ['item', key] });
    } catch (error) {
      setMessage(apiErrorMessage(error));
    }
  };

  const dropOn = async (category: StatusCategory, key: string) => {
    setMessage(undefined);
    if (items.find((item) => item.key === key)?.statusCategory === category) return;
    try {
      const item = await getItem(key);
      const choices = movesInto(item, category);
      if (choices.length === 0) {
        setMessage(`${key} cannot move from ${item.statusName} to ${categoryLabel(category)}.`);
      } else if (choices.length === 1) {
        await runTransition(key, choices[0].transition.id);
      } else {
        setPendingMove({ item, categoryLabel: categoryLabel(category), choices });
      }
    } catch (error) {
      setMessage(apiErrorMessage(error));
    }
  };

  const handleDrop = (event: DragEvent, category: StatusCategory) => {
    event.preventDefault();
    dropOn(category, event.dataTransfer.getData('text/plain'));
  };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
        <h1 style={{ fontSize: '1.4rem', margin: 0, flex: 1 }}>Board · {project.key}</h1>
        <label>
          Type:{' '}
          <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
            <option value={ALL}>All</option>
            {project.itemTypes.map((type) => (
              <option key={type.name} value={type.name}>
                {type.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Assignee:{' '}
          <select value={assigneeFilter} onChange={(event) => setAssigneeFilter(event.target.value)}>
            <option value={ALL}>All</option>
            {assignees.map((assignee) => (
              <option key={assignee} value={assignee}>
                {assignee}
              </option>
            ))}
          </select>
        </label>
      </div>
      {message && <p role="alert">{message}</p>}
      {data && !data.last && <p>Showing the {MAX_ITEMS} most recently updated items.</p>}
      <div style={{ display: 'flex', gap: '1rem', alignItems: 'stretch', minHeight: '70%' }}>
        {STATUS_CATEGORIES.map(({ value, label }) => {
          const category = value as StatusCategory;
          const cards = visibleItems.filter((item) => item.statusCategory === category);
          return (
            <section
              key={category}
              aria-label={label}
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => handleDrop(event, category)}
              style={{ flex: 1, background: '#e0e0e0', borderRadius: 4, padding: '0.5rem' }}
            >
              <h2 style={{ fontSize: '0.85rem', margin: '0 0 0.5rem' }}>
                {label.toUpperCase()} ({cards.length})
              </h2>
              {cards.map((item) => (
                <Card key={item.key} item={item} />
              ))}
            </section>
          );
        })}
      </div>
      {pendingMove && (
        <MovePicker
          move={pendingMove}
          onCancel={() => setPendingMove(undefined)}
          onConfirm={(transitionId) => runTransition(pendingMove.item.key, transitionId)}
        />
      )}
    </>
  );
}

export default function BoardPage() {
  const { currentProject } = useProjects();
  if (!currentProject) {
    return <p>There are no projects yet.</p>;
  }
  return <ProjectBoard key={currentProject.key} project={currentProject} />;
}

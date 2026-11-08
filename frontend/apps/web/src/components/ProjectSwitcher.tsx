import { useProjects } from '../projects/ProjectContext';

export default function ProjectSwitcher() {
  const { projects, currentProject, selectProject } = useProjects();

  if (projects.length === 0) {
    return <div style={{ fontSize: '0.85rem', color: '#ccc' }}>No projects</div>;
  }
  return (
    <select
      aria-label="Project"
      value={currentProject?.key}
      onChange={(event) => selectProject(event.target.value)}
      style={{ width: '100%', padding: '0.25rem' }}
    >
      {projects.map((project) => (
        <option key={project.key} value={project.key}>
          {project.key}
        </option>
      ))}
    </select>
  );
}

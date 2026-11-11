import { NativeSelect, Text } from '@mantine/core';
import { useProjects } from '../projects/ProjectContext';
import { SUBTLE_INK } from '../theme';

export default function ProjectSwitcher() {
  const { projects, currentProject, selectProject } = useProjects();

  if (projects.length === 0) {
    return (
      <Text size="sm" c={SUBTLE_INK}>
        No projects
      </Text>
    );
  }
  return (
    <NativeSelect
      label="Project"
      value={currentProject?.key}
      onChange={(event) => selectProject(event.target.value)}
      data={projects.map((project) => ({ value: project.key, label: `${project.name} (${project.key})` }))}
    />
  );
}

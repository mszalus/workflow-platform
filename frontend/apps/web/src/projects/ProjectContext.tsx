import { createContext, useContext, useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../shared-ui';

export interface Project {
  key: string;
  name: string;
  itemTypes: { name: string; workflowKey: string }[];
}

interface ProjectContextValue {
  projects: Project[];
  currentProject: Project | undefined;
  selectProject: (key: string) => void;
}

const SELECTED_PROJECT_STORAGE_KEY = 'wfp.selectedProject';

const ProjectContext = createContext<ProjectContextValue | undefined>(undefined);

export function ProjectProvider({ children }: { children: ReactNode }) {
  const { data: projects = [] } = useQuery({
    queryKey: ['projects'],
    queryFn: () => apiClient.get<Project[]>('/projects').then((response) => response.data),
  });
  const [selectedKey, setSelectedKey] = useState(() => localStorage.getItem(SELECTED_PROJECT_STORAGE_KEY));

  const selectProject = (key: string) => {
    localStorage.setItem(SELECTED_PROJECT_STORAGE_KEY, key);
    setSelectedKey(key);
  };
  const currentProject = projects.find((project) => project.key === selectedKey) ?? projects[0];

  return (
    <ProjectContext.Provider value={{ projects, currentProject, selectProject }}>{children}</ProjectContext.Provider>
  );
}

export function useProjects() {
  const context = useContext(ProjectContext);
  if (!context) {
    throw new Error('useProjects must be used within a ProjectProvider');
  }
  return context;
}

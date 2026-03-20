import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  listMyTasks,
  listUnassignedTasks,
  listCompletedTasks,
  getTask,
  claimTask,
  unclaimTask,
  completeTask,
  delegateTask,
  type TaskFilter,
  type CompleteTaskRequest,
  type DelegateTaskRequest,
} from "@/api/taskApi";

export function useMyTasks(filter: TaskFilter = {}) {
  return useQuery({
    queryKey: ["tasks", "my", filter],
    queryFn: () => listMyTasks(filter),
    staleTime: 30_000,
  });
}

export function useUnassignedTasks(filter: TaskFilter = {}) {
  return useQuery({
    queryKey: ["tasks", "unassigned", filter],
    queryFn: () => listUnassignedTasks(filter),
    staleTime: 30_000,
  });
}

export function useCompletedTasks(filter: TaskFilter = {}) {
  return useQuery({
    queryKey: ["tasks", "completed", filter],
    queryFn: () => listCompletedTasks(filter),
    staleTime: 30_000,
  });
}

export function useTask(id: string) {
  return useQuery({
    queryKey: ["tasks", id],
    queryFn: () => getTask(id),
    enabled: !!id,
  });
}

export function useClaimTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => claimTask(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ["tasks", id] });
      queryClient.invalidateQueries({ queryKey: ["tasks", "my"] });
      queryClient.invalidateQueries({ queryKey: ["tasks", "unassigned"] });
    },
  });
}

export function useUnclaimTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => unclaimTask(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ["tasks", id] });
      queryClient.invalidateQueries({ queryKey: ["tasks", "my"] });
      queryClient.invalidateQueries({ queryKey: ["tasks", "unassigned"] });
    },
  });
}

export function useCompleteTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: CompleteTaskRequest }) =>
      completeTask(id, request),
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({ queryKey: ["tasks", id] });
      queryClient.invalidateQueries({ queryKey: ["tasks", "my"] });
      queryClient.invalidateQueries({ queryKey: ["tasks", "completed"] });
    },
  });
}

export function useDelegateTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: DelegateTaskRequest }) =>
      delegateTask(id, request),
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({ queryKey: ["tasks", id] });
      queryClient.invalidateQueries({ queryKey: ["tasks", "my"] });
    },
  });
}

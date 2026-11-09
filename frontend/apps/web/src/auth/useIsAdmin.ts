import { useAuth } from '../shared-ui';

export function useIsAdmin() {
  return useAuth().roles.includes('admin');
}

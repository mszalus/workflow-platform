import { useAuth } from '@wfp/shared-ui';

export function useIsAdmin() {
  return useAuth().roles.includes('admin');
}

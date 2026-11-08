export const keycloakUrl =
  window.location.hostname === 'localhost' ? 'http://localhost:8180' : `http://${window.location.hostname}:8180`;

export const keycloakUsersUrl = `${keycloakUrl}/admin/master/console/#/workflow-platform/users`;

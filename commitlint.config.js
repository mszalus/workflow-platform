export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    // Allow longer subject lines (default is 72)
    'header-max-length': [2, 'always', 120],
    // Allow body lines up to 120 chars
    'body-max-line-length': [1, 'always', 120],
  },
};

let _token: string | null = null;

export const setToken = (t: string | null): void => {
  _token = t;
};

export const getToken = (): string | null => _token;

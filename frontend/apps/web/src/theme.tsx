import type { ReactNode } from 'react';
import { createTheme, MantineProvider } from '@mantine/core';
import '@mantine/core/styles.css';

export const INK = '#172b4d';
export const SUBTLE_INK = '#5e6c84';
export const SURFACE = '#f4f5f7';
export const BORDER = '#dfe1e6';

const theme = createTheme({
  primaryColor: 'brand',
  primaryShade: 6,
  colors: {
    brand: [
      '#deebff',
      '#b3d4ff',
      '#4c9aff',
      '#2684ff',
      '#0065ff',
      '#0052cc',
      '#0052cc',
      '#0747a6',
      '#0747a6',
      '#053581',
    ],
  },
  black: INK,
  defaultRadius: 3,
  fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif",
  headings: { fontWeight: '500' },
});

export function ThemeProvider({ children }: { children: ReactNode }) {
  return <MantineProvider theme={theme}>{children}</MantineProvider>;
}

import type { ReactNode } from 'react';

// i18n, query client, toast dùng chung
export function Providers({ children }: { children?: ReactNode }) {
  return <>{children}</>; // TODO: QueryClientProvider, i18n, toast
}

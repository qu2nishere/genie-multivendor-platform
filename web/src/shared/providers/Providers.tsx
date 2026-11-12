// Providers dùng chung cho island lớn và SPA (plan G05.09; TS 2.4, 5.3):
// QueryClientProvider (một client cho mỗi cây React), khởi tạo locale, khởi động phiên.
// Vùng hiện toast dùng chung là ToastHost (layouts/storefront/ToastHost.tsx, AppShell) — G06.14, G07.07.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useEffect, useState, type ReactNode } from 'react';
import { isApiError } from '@/shared/api';
import { bootstrapSession } from '@/shared/auth/session';
import { $locale, toLocale } from '@/shared/lib/locale';

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        // Không thử lại lỗi 4xx (sai quyền, không tìm thấy…); lỗi mạng / 5xx thử lại 1 lần
        retry: (count, err) =>
          !(isApiError(err) && err.status >= 400 && err.status < 500) && count < 1,
      },
      mutations: { retry: false },
    },
  });
}

export function Providers({ children, locale }: { children?: ReactNode; locale?: string }) {
  const [client] = useState(createQueryClient);

  useEffect(() => {
    const l = toLocale(locale ?? document.documentElement.lang);
    if ($locale.get() !== l) $locale.set(l);
    void bootstrapSession();
  }, [locale]);

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

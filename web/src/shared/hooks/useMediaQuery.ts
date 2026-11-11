import { useSyncExternalStore } from 'react';

/** Điểm gãy khớp tailwind.config.ts (prototype: max-width 480 / 768 / 1100). */
export const MQ = {
  phone: '(max-width: 480px)',
  narrow: '(max-width: 768px)',
  tablet: '(max-width: 1100px)',
  reducedMotion: '(prefers-reduced-motion: reduce)',
} as const;

/** true khi media query khớp; SSR luôn trả `serverValue` (mặc định false) để tránh lệch hydrate. */
export function useMediaQuery(query: string, serverValue = false): boolean {
  return useSyncExternalStore(
    (cb) => {
      const mql = window.matchMedia(query);
      mql.addEventListener('change', cb);
      return () => mql.removeEventListener('change', cb);
    },
    () => window.matchMedia(query).matches,
    () => serverValue,
  );
}

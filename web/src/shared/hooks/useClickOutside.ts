import { useEffect, type RefObject } from 'react';

/** Gọi `onOutside` khi bấm / chạm ngoài các phần tử trong `refs` (từ Select của prototype). */
export function useClickOutside(
  refs: RefObject<HTMLElement | null> | RefObject<HTMLElement | null>[],
  onOutside: (e: PointerEvent) => void,
  enabled = true,
): void {
  useEffect(() => {
    if (!enabled) return undefined;
    const list = Array.isArray(refs) ? refs : [refs];
    const handler = (e: PointerEvent) => {
      const target = e.target as Node | null;
      if (!target) return;
      if (list.some((r) => r.current?.contains(target))) return;
      onOutside(e);
    };
    document.addEventListener('pointerdown', handler);
    return () => document.removeEventListener('pointerdown', handler);
  }, [refs, onOutside, enabled]);
}

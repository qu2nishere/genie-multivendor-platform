import { useEffect, type RefObject } from 'react';

const FOCUSABLE =
  'a[href], area[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), iframe, [tabindex]:not([tabindex="-1"]), [contenteditable="true"]';

export const focusableIn = (root: HTMLElement): HTMLElement[] =>
  Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
    (el) => !el.hasAttribute('inert') && el.getClientRects().length > 0,
  );

/**
 * Giữ focus trong `ref` khi `active` (từ Modal của prototype): Tab / Shift+Tab vòng trong hộp,
 * focus phần tử đầu (hoặc `initialFocus`) khi mở, trả focus về chỗ cũ khi đóng.
 */
export function useFocusTrap(
  ref: RefObject<HTMLElement | null>,
  active: boolean,
  initialFocus?: RefObject<HTMLElement | null>,
): void {
  useEffect(() => {
    if (!active) return undefined;
    const root = ref.current;
    if (!root) return undefined;
    const previous = document.activeElement as HTMLElement | null;
    const first = initialFocus?.current ?? focusableIn(root)[0] ?? root;
    first.focus({ preventScroll: true });

    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;
      const items = focusableIn(root);
      if (items.length === 0) {
        e.preventDefault();
        root.focus();
        return;
      }
      const head = items[0];
      const tail = items[items.length - 1];
      const cur = document.activeElement;
      if (e.shiftKey && (cur === head || !root.contains(cur))) {
        e.preventDefault();
        tail.focus();
      } else if (!e.shiftKey && (cur === tail || !root.contains(cur))) {
        e.preventDefault();
        head.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      if (previous && document.contains(previous)) previous.focus({ preventScroll: true });
    };
  }, [ref, active, initialFocus]);
}

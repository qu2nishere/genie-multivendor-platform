// Toast dùng chung mọi island (plan G06.14; TS 5.3): nanostore $toasts thay các hàm toast() rải ở
// 4 file prototype và sự kiện window 'show_toast'. Island nào cũng gọi được toast.success(...);
// ToastViewport (trong ToastHost của layout) hiển thị.
import { atom } from 'nanostores';

export type ToastType = 'info' | 'success' | 'error';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
  /** ms; 0 = không tự đóng. */
  duration: number;
}

export const $toasts = atom<Toast[]>([]);

let seq = 0;
const timers = new Map<number, ReturnType<typeof setTimeout>>();
const MAX_VISIBLE = 3;

export function dismissToast(id: number): void {
  const t = timers.get(id);
  if (t) clearTimeout(t);
  timers.delete(id);
  $toasts.set($toasts.get().filter((x) => x.id !== id));
}

function push(type: ToastType, message: string, duration = type === 'error' ? 5000 : 3000): number {
  const id = ++seq;
  const next = [...$toasts.get(), { id, type, message, duration }];
  // Giữ tối đa 3 toast, bỏ cái cũ nhất
  while (next.length > MAX_VISIBLE) {
    const old = next.shift();
    if (old) dismissToast(old.id);
  }
  $toasts.set(next);
  if (duration > 0 && typeof window !== 'undefined') {
    timers.set(
      id,
      setTimeout(() => dismissToast(id), duration),
    );
  }
  return id;
}

export const toast = {
  info: (message: string, duration?: number) => push('info', message, duration),
  success: (message: string, duration?: number) => push('success', message, duration),
  error: (message: string, duration?: number) => push('error', message, duration),
  dismiss: dismissToast,
};

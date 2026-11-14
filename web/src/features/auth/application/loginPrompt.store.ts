// Hộp nhắc đăng nhập dùng chung mọi island (thay sự kiện window 'lz_require_login' của prototype).
import { atom } from 'nanostores';

export const $loginPrompt = atom<{ open: boolean; next: string | null }>({
  open: false,
  next: null,
});

/** Khách bấm thao tác cần tài khoản (yêu thích, theo dõi, chat, thanh toán…). */
export function requireLogin(next?: string): void {
  const here =
    typeof window !== 'undefined' ? window.location.pathname + window.location.search : '/';
  $loginPrompt.set({ open: true, next: next ?? here });
}

export function closeLoginPrompt(): void {
  $loginPrompt.set({ open: false, next: null });
}

export const loginHref = (next?: string | null): string =>
  next ? `/auth/login?next=${encodeURIComponent(next)}` : '/auth/login';

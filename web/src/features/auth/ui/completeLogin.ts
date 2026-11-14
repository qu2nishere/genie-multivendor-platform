// Ghép sau đăng nhập ở tầng UI: lưu phiên → gộp giỏ khách (feature cart) → chuyển về `next`.
import { mergeGuestCart } from '@/features/cart';
import type { SessionUser } from '@/shared/auth/session';
import { finishLogin } from '../application/useAuth';
import type { AuthSession } from '../domain/auth.types';

export const completeLogin = (session: AuthSession<SessionUser>, next: string | null) =>
  finishLogin(session, next, mergeGuestCart);

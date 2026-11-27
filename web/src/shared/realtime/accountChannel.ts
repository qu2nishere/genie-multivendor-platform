// Kênh realtime /user/queue/account (API_SPEC mục 13, plan B1): khi phiên bị thu hồi phía server — bị khóa, bị xóa,
// đặt lại mật khẩu — mọi tab đang mở đăng xuất ngay, không phải tải lại trang; trang đăng nhập đọc lý do để báo.
// Đổi quyền / chức danh nhân viên KHÔNG đăng xuất (D19): chỉ phát sự kiện để khung Admin tải lại menu.
// Từ plan 2026-11-26 (thông báo N05.02): đi qua hub dùng chung (realtimeHub) thay vì tự giữ một StompConnection.
import { clearSession } from '@/shared/auth/session';
import { closeRealtime, startRealtime, subscribe } from './realtimeHub';

export { wsUrlOf } from './realtimeHub';

export const ACCOUNT_QUEUE = '/user/queue/account';
/**
 * Sự kiện cửa sổ khi Admin đổi quyền / chức danh của chính mình (plan B1 D19 — phiên vẫn giữ). Khung Admin nghe sự kiện
 * này để tải lại `/admin/me` và menu.
 */
export const PERMISSIONS_CHANGED_EVENT = 'genie:permissions-changed';
const FORCED_LOGOUT_KEY = 'genie.forced_logout';
/** Lý do có câu báo riêng (i18n `auth.forced_logout.*`); lý do khác dùng `DEFAULT`. */
export const FORCED_LOGOUT_REASONS = [
  'SUSPENDED',
  'ACCOUNT_DELETED',
  'ROLE_CHANGED',
  'PASSWORD_CHANGED',
] as const;
export type ForcedLogoutReason = (typeof FORCED_LOGOUT_REASONS)[number] | 'DEFAULT';

export function toForcedLogoutReason(raw: unknown): ForcedLogoutReason {
  return (FORCED_LOGOUT_REASONS as readonly unknown[]).includes(raw)
    ? (raw as ForcedLogoutReason)
    : 'DEFAULT';
}

let started = false;

/** Gọi một lần từ Providers: theo dõi phiên qua hub — đăng nhập thì mở kênh, đăng xuất thì đóng. Chế độ mock bỏ qua. */
export function startAccountChannel(): void {
  if (started || typeof window === 'undefined') return;
  started = true;
  startRealtime();
  subscribe(ACCOUNT_QUEUE, (body) => {
    const msg = body as { type?: string; reason?: string } | null;
    if (msg?.type === 'SESSION_REVOKED') forceLogout(toForcedLogoutReason(msg.reason));
    else if (msg?.type === 'PERMISSIONS_CHANGED')
      window.dispatchEvent(new CustomEvent(PERMISSIONS_CHANGED_EVENT));
  });
}

function forceLogout(reason: ForcedLogoutReason): void {
  closeRealtime();
  try {
    sessionStorage.setItem(FORCED_LOGOUT_KEY, reason);
  } catch {
    // sessionStorage bị chặn — vẫn đăng xuất
  }
  clearSession();
  window.location.assign('/auth/login');
}

/** Trang đăng nhập: lấy (và xóa) lý do vừa bị đăng xuất — `null` nếu không có. */
export function takeForcedLogoutReason(): ForcedLogoutReason | null {
  try {
    const v = sessionStorage.getItem(FORCED_LOGOUT_KEY);
    if (v === null) return null;
    sessionStorage.removeItem(FORCED_LOGOUT_KEY);
    return toForcedLogoutReason(v);
  } catch {
    return null;
  }
}

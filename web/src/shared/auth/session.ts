// Phiên đăng nhập (plan G05.07; TS 5.3, 9; LUU_TRU_DU_LIEU 6.4).
// - Access token CHỈ nằm trong biến bộ nhớ; tải lại trang → POST /auth/sessions/refresh
//   (trình duyệt tự gửi cookie HttpOnly genie_rt).
// - Cookie thường `genie_logged_in=1` chỉ là gợi ý cho middleware chuyển hướng (UX), không phải bằng chứng.
// - Chế độ mock (D3): refresh trả người dùng mẫu khi cờ cookie đang bật.
import { atom } from 'nanostores';
import { env } from '@/shared/config/env';
import { clearUserData } from '@/shared/lib/storage';
import { PERMISSIONS, type PermissionCode, type Role } from './permissions';

export const LOGGED_IN_COOKIE = 'genie_logged_in';

export type KycStatus =
  'NOT_SUBMITTED' | 'PENDING' | 'SUPPLEMENT_REQUESTED' | 'REJECTED' | 'VERIFIED';

export interface SessionUser {
  id: number;
  name: string;
  email: string;
  phone?: string;
  avatar_url?: string | null;
  roles: Role[];
  /** Mã quyền của STAFF / ADMIN (trống với người mua / người bán). */
  permissions: PermissionCode[];
  shop_id?: number;
  shop_name?: string;
  kyc_status?: KycStatus;
  has_password?: boolean;
}

export type Session =
  { status: 'unknown' } | { status: 'guest' } | { status: 'authenticated'; user: SessionUser };

export const $session = atom<Session>({ status: 'unknown' });

let accessToken: string | null = null;

export const getAccessToken = (): string | null => accessToken;

function setLoggedInCookie(on: boolean): void {
  if (typeof document === 'undefined') return;
  document.cookie = on
    ? `${LOGGED_IN_COOKIE}=1; path=/; max-age=${7 * 24 * 3600}; SameSite=Lax`
    : `${LOGGED_IN_COOKIE}=; path=/; max-age=0; SameSite=Lax`;
}

export function hasLoggedInHint(): boolean {
  return (
    typeof document !== 'undefined' &&
    document.cookie.split('; ').some((c) => c === `${LOGGED_IN_COOKIE}=1`)
  );
}

/** Gọi sau đăng nhập / làm mới thành công. */
export function setSession(token: string, user: SessionUser): void {
  accessToken = token;
  setLoggedInCookie(true);
  $session.set({ status: 'authenticated', user });
}

/** Đăng xuất phía client: xóa token, cờ cookie, dữ liệu người dùng trong storage (6.6). */
export function clearSession(): void {
  accessToken = null;
  setLoggedInCookie(false);
  clearUserData();
  $session.set({ status: 'guest' });
}

/** Người mua + chủ shop mẫu khi chạy mock (prototype storefront/data.jsx USER). */
export const MOCK_USER: SessionUser = {
  id: 20411,
  name: 'Trần Thị Mai',
  email: 'mai.tran@gmail.com',
  phone: '0901234567',
  avatar_url: null,
  roles: ['CUSTOMER', 'VENDOR'],
  permissions: [],
  shop_id: 1,
  shop_name: 'TechZone Official',
  kyc_status: 'VERIFIED',
  has_password: false,
};

/** Admin mẫu đủ 13 quyền cho /admin (prototype admin/data.jsx CURRENT_USER; plan G12.05). */
export const MOCK_STAFF: SessionUser = {
  id: 1,
  name: 'Đặng Hải Yến',
  email: 'yen.dh@genie.example',
  avatar_url: null,
  roles: ['ADMIN'],
  permissions: [...PERMISSIONS],
  has_password: true,
};

/** Mock: tài khoản nhân viên tách khỏi tài khoản người mua, chọn theo bề mặt đang mở. */
export const mockUserFor = (pathname: string): SessionUser =>
  pathname.startsWith('/admin') ? MOCK_STAFF : MOCK_USER;

let refreshing: Promise<boolean> | null = null;

interface RefreshResponse {
  data: { access_token: string; user: SessionUser };
}

/**
 * Làm mới access token MỘT lần (các lời gọi đồng thời dùng chung một promise).
 * Trả true nếu có phiên mới.
 */
export function refreshAccessToken(): Promise<boolean> {
  if (refreshing) return refreshing;
  refreshing = (async () => {
    try {
      if (env.PUBLIC_USE_MOCK) {
        if (!hasLoggedInHint()) {
          $session.set({ status: 'guest' });
          return false;
        }
        setSession('mock-access-token', mockUserFor(window.location.pathname));
        return true;
      }
      const res = await fetch(`${env.PUBLIC_API_URL}/auth/sessions/refresh`, {
        method: 'POST',
        credentials: 'include',
        headers: { Accept: 'application/json' },
      });
      if (!res.ok) {
        clearSession();
        return false;
      }
      const body = (await res.json()) as RefreshResponse;
      setSession(body.data.access_token, body.data.user);
      return true;
    } catch {
      $session.set({ status: 'guest' });
      return false;
    } finally {
      refreshing = null;
    }
  })();
  return refreshing;
}

/** Khởi động phiên khi island đầu tiên chạy: có cờ đăng nhập thì thử refresh, không thì là khách. */
export function bootstrapSession(): Promise<boolean> {
  const s = $session.get();
  if (s.status !== 'unknown') return Promise.resolve(s.status === 'authenticated');
  if (!hasLoggedInHint()) {
    $session.set({ status: 'guest' });
    return Promise.resolve(false);
  }
  return refreshAccessToken();
}

export const currentUser = (): SessionUser | null => {
  const s = $session.get();
  return s.status === 'authenticated' ? s.user : null;
};

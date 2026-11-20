// Phiên đăng nhập (plan G05.07; TS 5.3, 9; LUU_TRU_DU_LIEU 6.4).
// - Access token CHỈ nằm trong biến bộ nhớ; tải lại trang → POST /auth/sessions/refresh
//   (trình duyệt tự gửi cookie HttpOnly genie_rt).
// - Cookie thường `genie_logged_in=1` chỉ là gợi ý cho middleware chuyển hướng (UX), không phải bằng chứng.
// - Chế độ mock (D3): refresh trả người dùng mẫu khi cờ cookie đang bật.
// - API thật: sau đăng nhập / làm mới, gọi GET /me để dựng SessionUser đầy đủ — email, vai trò theo cấp bậc, shop,
//   có mật khẩu chưa (plan AUTH D18, A15.01–A15.02). /me lỗi → giữ thông tin tối thiểu của $AuthTokens.
import { atom } from 'nanostores';
import { env, isMocked } from '@/shared/config/env';
import { clearUserData } from '@/shared/lib/storage';
import { PERMISSIONS, TITLE_PRESETS, type PermissionCode, type Role } from './permissions';

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

/** Nhân viên mẫu theo chức danh (trùng danh sách Phân quyền nhân viên của mock Admin). */
const MOCK_STAFF_BY_TITLE = {
  MODERATOR: { id: 7, name: 'Ngô Tấn Phát', email: 'phat.nt@genie.example' },
  FINANCE_OFFICER: { id: 8, name: 'Trịnh Mỹ Linh', email: 'linh.tm@genie.example' },
  CS_SPECIALIST: { id: 10, name: 'Hồ Thanh Trúc', email: 'truc.ht@genie.example' },
} as const;
type MockStaffTitle = keyof typeof MOCK_STAFF_BY_TITLE;

/**
 * Mock: đăng nhập /admin bằng nhân viên có chức danh (sessionStorage `genie.mock.staff_title`).
 * Chỉ dùng cho e2e kiểm phân quyền (G13.16) — không có nút đổi vai trên giao diện (D5).
 */
export const MOCK_STAFF_TITLE_KEY = 'genie.mock.staff_title';

function mockStaff(): SessionUser {
  let title: string | null = null;
  try {
    title = sessionStorage.getItem(MOCK_STAFF_TITLE_KEY);
  } catch {
    // trình duyệt chặn storage → dùng Admin mẫu
  }
  if (!title || !(title in MOCK_STAFF_BY_TITLE)) return MOCK_STAFF;
  const p = MOCK_STAFF_BY_TITLE[title as MockStaffTitle];
  return {
    ...p,
    avatar_url: null,
    roles: ['STAFF'],
    permissions: [...TITLE_PRESETS[title as MockStaffTitle]],
    has_password: true,
  };
}

/** Mock: tài khoản nhân viên tách khỏi tài khoản người mua, chọn theo bề mặt đang mở. */
export const mockUserFor = (pathname: string): SessionUser =>
  pathname.startsWith('/admin') ? mockStaff() : MOCK_USER;

let refreshing: Promise<boolean> | null = null;

/** $AuthTokens (API_SPEC 1) — backend trả kèm `user` tối thiểu cả khi làm mới. */
interface RefreshResponse {
  data: {
    access_token: string;
    user: { id: number; role: Role; full_name: string; avatar_url: string | null };
  };
}

/** `user` của $AuthTokens → SessionUser (email, quyền chi tiết nạp sau từ /me khi có). */
const fromAuthUser = (u: RefreshResponse['data']['user']): SessionUser => ({
  id: u.id,
  name: u.full_name,
  email: '',
  avatar_url: u.avatar_url,
  roles: [u.role],
  permissions: [],
});

/** `GET /me` (API_SPEC 3.1). */
export interface MeDto {
  id: number;
  role: Role;
  full_name: string;
  avatar_url: string | null;
  email: string;
  login_methods: { password: boolean; google: boolean; zalo?: boolean };
  shop: { id: number; name: string; logo_url: string | null; kyc_status: string } | null;
  counters: {
    cart_items: number;
    unread_notifications: number;
    unread_chats: number;
    available_vouchers: number;
    following_shops: number;
  };
}

/** Trạng thái KYC của backend → các trạng thái web hiển thị (AI_PROCESSING, MANUAL_REVIEW = đang xét). */
function toKycStatus(s: string | undefined): KycStatus | undefined {
  switch (s) {
    case 'NOT_SUBMITTED':
    case 'PENDING':
    case 'SUPPLEMENT_REQUESTED':
    case 'REJECTED':
    case 'VERIFIED':
      return s;
    case 'AI_PROCESSING':
    case 'MANUAL_REVIEW':
      return 'PENDING';
    default:
      return undefined;
  }
}

/**
 * `GET /me` → SessionUser. Vai trò theo cấp bậc (RoleHierarchy backend): VENDOR cũng là CUSTOMER (chủ shop vẫn mua
 * hàng). Quyền chi tiết của STAFF chờ `GET /admin/me/permissions` (module admin); tạm thời ADMIN = đủ 13 quyền.
 */
export function sessionUserFromMe(me: MeDto): SessionUser {
  const roles: Role[] = me.role === 'VENDOR' ? ['CUSTOMER', 'VENDOR'] : [me.role];
  const kyc = toKycStatus(me.shop?.kyc_status);
  return {
    id: me.id,
    name: me.full_name,
    email: me.email,
    avatar_url: me.avatar_url,
    roles,
    permissions: me.role === 'ADMIN' ? [...PERMISSIONS] : [],
    ...(me.shop ? { shop_id: me.shop.id, shop_name: me.shop.name } : {}),
    ...(kyc ? { kyc_status: kyc } : {}),
    has_password: me.login_methods.password,
  };
}

/** Gọi `GET /me` bằng access token vừa nhận; lỗi / không có mạng → null. */
async function fetchMe(token: string): Promise<MeDto | null> {
  try {
    const res = await fetch(`${env.PUBLIC_API_URL}/me`, {
      headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      credentials: 'include',
    });
    if (!res.ok) return null;
    const body = (await res.json()) as { data?: MeDto };
    return body.data ?? null;
  } catch {
    return null;
  }
}

/**
 * Bổ sung SessionUser tối thiểu (từ $AuthTokens) bằng `GET /me` — dùng ngay sau đăng nhập (plan AUTH A15.01).
 * Mock: giữ nguyên người dùng mẫu.
 */
export async function loadSessionUser(token: string, fallback: SessionUser): Promise<SessionUser> {
  if (isMocked('auth')) return fallback;
  const me = await fetchMe(token);
  return me ? sessionUserFromMe(me) : fallback;
}

/**
 * Làm mới access token MỘT lần (các lời gọi đồng thời dùng chung một promise).
 * Trả true nếu có phiên mới.
 */
export function refreshAccessToken(): Promise<boolean> {
  if (refreshing) return refreshing;
  refreshing = (async () => {
    try {
      if (isMocked('auth')) {
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
      const token = body.data.access_token;
      setSession(token, await loadSessionUser(token, fromAuthUser(body.data.user)));
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

/**
 * Tài khoản nội bộ (STAFF / ADMIN) — chỉ xem storefront, không mua hàng (plan B1 D16 hướng B; server chặn giỏ hàng /
 * đặt hàng bằng 403 GNE-ORD-4031). Mua sắm bằng tài khoản khách hàng cá nhân.
 */
export function isInternalUser(user: Pick<SessionUser, 'roles'> | null | undefined): boolean {
  return !!user && (user.roles.includes('ADMIN') || user.roles.includes('STAFF'));
}

/** Trang chỉ dành cho người mua — tài khoản nội bộ mở sẽ được đưa về trang chủ. */
export const BUYER_ONLY_PATHS = ['/cart', '/checkout', '/orders', '/account'] as const;

export function isBuyerOnlyPath(path: string): boolean {
  return BUYER_ONLY_PATHS.some((p) => path === p || path.startsWith(`${p}/`));
}

export const currentUser = (): SessionUser | null => {
  const s = $session.get();
  return s.status === 'authenticated' ? s.user : null;
};

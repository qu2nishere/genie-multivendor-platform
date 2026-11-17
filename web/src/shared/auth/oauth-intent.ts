// Ý định của lần chuyển sang nhà cung cấp (Google / Zalo): đăng nhập (mặc định) hay LIÊN KẾT vào tài khoản đang đăng
// nhập (Tài khoản › Liên kết đăng nhập — plan AUTH A14.04), và nhà cung cấp nào. Lưu sessionStorage của tab: trang
// `/auth/oauth-callback` (callback cố định, không kèm tham số) đọc một lần để gọi đúng API
// (`POST /auth/oauth/{provider}/token` hay `POST /me/identities/{provider}`).
const KEY = 'genie.oauth.intent';

export interface OAuthIntent {
  /** `google` | `zalo`; null = không rõ (callback mặc định Google). */
  provider: string | null;
  link: boolean;
}

export function markOAuthIntent(provider: string, link = false): void {
  try {
    window.sessionStorage.setItem(KEY, JSON.stringify({ provider, link }));
  } catch {
    // sessionStorage bị chặn (chế độ riêng tư) → callback xử lý như đăng nhập Google, backend từ chối state lạ (4001)
  }
}

/** Đánh dấu ý định liên kết (giữ cho chỗ gọi cũ). */
export function markOAuthLinkIntent(provider = 'google'): void {
  markOAuthIntent(provider, true);
}

/** Đọc và xóa ngay — gọi một lần ở trang callback. */
export function takeOAuthIntent(): OAuthIntent {
  try {
    const raw = window.sessionStorage.getItem(KEY);
    window.sessionStorage.removeItem(KEY);
    if (!raw) return { provider: null, link: false };
    if (raw === 'link') return { provider: null, link: true }; // dạng cũ
    const v = JSON.parse(raw) as Partial<OAuthIntent>;
    return { provider: typeof v.provider === 'string' ? v.provider : null, link: v.link === true };
  } catch {
    return { provider: null, link: false };
  }
}

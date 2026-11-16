// Ý định của lần chuyển sang Google: đăng nhập (mặc định) hay LIÊN KẾT Google vào tài khoản đang đăng nhập
// (Tài khoản › Liên kết đăng nhập — plan AUTH A14.04). Lưu sessionStorage của tab: trang `/auth/oauth-callback`
// đọc một lần để gọi đúng API (`POST /auth/oauth/google/token` hay `POST /me/identities/google`).
const KEY = 'genie.oauth.intent';

export function markOAuthLinkIntent(): void {
  try {
    window.sessionStorage.setItem(KEY, 'link');
  } catch {
    // sessionStorage bị chặn (chế độ riêng tư) → callback xử lý như đăng nhập, backend từ chối state liên kết (4001)
  }
}

/** Đọc và xóa ngay — gọi một lần ở trang callback. */
export function takeOAuthLinkIntent(): boolean {
  try {
    const v = window.sessionStorage.getItem(KEY);
    window.sessionStorage.removeItem(KEY);
    return v === 'link';
  } catch {
    return false;
  }
}

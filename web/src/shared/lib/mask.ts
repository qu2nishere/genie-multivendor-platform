// Che dữ liệu cá nhân khi hiển thị (plan G04.04; TS mục 9 — dạng 0792******34).

/** "nguyenvana@gmail.com" → "ng****@gmail.com" (prototype Account.jsx maskEmail). */
export function maskEmail(email: string | null | undefined): string {
  const e = (email ?? '').trim();
  const at = e.lastIndexOf('@');
  if (at < 1) return e;
  const name = e.slice(0, at);
  return name.slice(0, Math.min(2, name.length)) + '****' + e.slice(at);
}

/** Giữ `head` ký tự đầu và `tail` ký tự cuối, phần giữa thay bằng *. */
function maskMiddle(raw: string, head: number, tail: number): string {
  const s = raw.replace(/\s+/g, '');
  if (s.length <= head + tail) return s;
  return s.slice(0, head) + '*'.repeat(s.length - head - tail) + s.slice(s.length - tail);
}

/** "0792123434" → "0792****34". */
export function maskPhone(phone: string | null | undefined): string {
  return maskMiddle(phone ?? '', 4, 2);
}

/** Số tài khoản ngân hàng / CCCD: "079212345634" → "0792******34". */
export function maskAccount(account: string | null | undefined): string {
  return maskMiddle(account ?? '', 4, 2);
}

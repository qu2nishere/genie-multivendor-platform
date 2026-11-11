// Luật mật khẩu dùng chung (TS 9: ≥ 8 ký tự có hoa, thường, số, ký tự đặc biệt).
// Thuần TS: PasswordInput (shared/ui) dùng độ mạnh; features/auth/domain dùng lại passwordIssue.

export type PasswordIssue = 'length' | 'upper' | 'lower' | 'digit' | 'special';
export type PasswordStrength = 0 | 1 | 2 | 3 | 4;

/** Lỗi đầu tiên cần sửa (prototype auth passwordIssue), null nếu đạt. */
export function passwordIssue(p: string): PasswordIssue | null {
  if (p.length < 8) return 'length';
  if (!/[A-Z]/.test(p)) return 'upper';
  if (!/[a-z]/.test(p)) return 'lower';
  if (!/[0-9]/.test(p)) return 'digit';
  if (!/[^A-Za-z0-9]/.test(p)) return 'special';
  return null;
}

/** Độ mạnh 0–4: +1 cho mỗi điều kiện ≥ 8 ký tự, có hoa, có số, có ký tự đặc biệt (prototype). */
export function passwordStrength(p: string): PasswordStrength {
  let s = 0;
  if (p.length >= 8) s++;
  if (/[A-Z]/.test(p)) s++;
  if (/[0-9]/.test(p)) s++;
  if (/[^A-Za-z0-9]/.test(p)) s++;
  return s as PasswordStrength;
}

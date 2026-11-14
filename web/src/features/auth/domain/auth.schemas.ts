// Lược đồ form xác thực (zod) — plan G09.02. Thông điệp lỗi là khóa i18n, UI tự dịch.
import { z } from 'zod';
import { passwordIssue } from '@/shared/lib/password';
import { isEmail, isPhoneVN } from './auth.rules';

const email = z
  .string()
  .trim()
  .min(1, 'auth.err.email_required')
  .refine(isEmail, 'auth.err.email_invalid');

const strongPassword = z.string().superRefine((p, ctx) => {
  const issue = passwordIssue(p);
  if (issue) ctx.addIssue({ code: z.ZodIssueCode.custom, message: `ui.password.issue.${issue}` });
});

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'auth.err.password_required'),
});

export const otpLoginSchema = z.object({ email });

export const registerSchema = z
  .object({
    name: z.string().trim().min(1, 'auth.err.name_required').max(100, 'auth.err.name_long'),
    email,
    phone: z.string().refine(isPhoneVN, 'auth.err.phone_invalid'),
    password: strongPassword,
    confirm: z.string(),
    agree: z.literal(true, { errorMap: () => ({ message: 'auth.err.agree' }) }),
  })
  .refine((v) => v.confirm === v.password, { path: ['confirm'], message: 'ui.password.mismatch' });

export const forgotSchema = z.object({ email });

export const resetSchema = z
  .object({ password: strongPassword, confirm: z.string() })
  .refine((v) => v.confirm === v.password, { path: ['confirm'], message: 'ui.password.mismatch' });

export type LoginInput = z.infer<typeof loginSchema>;
export type RegisterInput = z.infer<typeof registerSchema>;
export type ResetInput = z.infer<typeof resetSchema>;

/** Lỗi đầu tiên của từng trường → khóa i18n (form hiện dưới ô). */
export function fieldErrors(err: z.ZodError): Record<string, string> {
  const out: Record<string, string> = {};
  for (const i of err.issues) {
    const k = String(i.path[0] ?? '_');
    if (!out[k]) out[k] = i.message;
  }
  return out;
}

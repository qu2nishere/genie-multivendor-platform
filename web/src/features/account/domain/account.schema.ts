// Lược đồ form khu tài khoản (zod) — thông điệp là khóa i18n.
import { z } from 'zod';
import { passwordIssue } from '@/shared/lib/password';
import { normalizePhone, PHONE_RE } from './address.rules';
import { GENDERS } from './account.types';

export const profileSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(1, 'account.profile.err.name')
    .max(100, 'account.profile.err.name_long'),
  phone: z
    .string()
    .transform(normalizePhone)
    .refine((p) => p === '' || PHONE_RE.test(p), 'account.address.err.phone'),
  gender: z.enum(GENDERS as [string, ...string[]]).nullable(),
  dateOfBirth: z
    .string()
    .refine(
      (d) => d === '' || (!Number.isNaN(Date.parse(d)) && Date.parse(d) < Date.now()),
      'account.profile.err.dob',
    ),
  bio: z.string().max(200, 'account.profile.err.bio'),
  language: z.enum(['vi', 'en']),
});
export type ProfileFormValues = z.infer<typeof profileSchema>;

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
export const emailChangeSchema = (current: string) =>
  z.object({
    email: z
      .string()
      .trim()
      .toLowerCase()
      .refine((m) => EMAIL_RE.test(m), 'auth.err.email_invalid')
      .refine((m) => m !== current.toLowerCase(), 'account.email.err.same'),
  });

/** Đổi / thiết lập mật khẩu: tài khoản chỉ có Google thì không cần mật khẩu hiện tại. */
export const passwordSchema = (hasPassword: boolean) =>
  z
    .object({
      current: z.string(),
      next: z.string().superRefine((p, ctx) => {
        const issue = passwordIssue(p);
        if (issue)
          ctx.addIssue({ code: z.ZodIssueCode.custom, message: `ui.password.issue.${issue}` });
      }),
      confirm: z.string(),
    })
    .refine((v) => !hasPassword || v.current.length > 0, {
      path: ['current'],
      message: 'account.password.err.current',
    })
    .refine((v) => !hasPassword || v.next !== v.current, {
      path: ['next'],
      message: 'account.password.err.same',
    })
    .refine((v) => v.confirm === v.next, { path: ['confirm'], message: 'ui.password.mismatch' });
export type PasswordFormValues = { current: string; next: string; confirm: string };

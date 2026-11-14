// Form mời nhân viên, khóa nhân viên, đình chỉ / mở khóa người dùng, tên tài khoản (zod; API 11.1,
// 11.13, 11.18).
import { z } from 'zod';
import { STAFF_EMAIL, SUSPEND_REASONS } from './system.rules';

export const inviteSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(1, 'admin-system.staff.err.name')
    .max(100, 'admin-system.staff.err.name'),
  email: z.string().trim().toLowerCase().regex(STAFF_EMAIL, 'admin-system.staff.err.email'),
  staffTitle: z.enum(['MODERATOR', 'CS_SPECIALIST', 'FINANCE_OFFICER', 'CUSTOM']),
});
export type InviteValues = z.infer<typeof inviteSchema>;

const reason = z
  .string()
  .trim()
  .min(1, 'admin-system.common.err.reason')
  .max(500, 'admin-system.common.err.long');

export const lockStaffSchema = z.object({ reason });
export type LockStaffValues = z.infer<typeof lockStaffSchema>;

export const suspendSchema = z
  .object({
    reason: z.enum(SUSPEND_REASONS),
    duration: z.enum(['P7D', 'P30D', 'FOREVER']),
    note: z.string().trim().max(500, 'admin-system.common.err.long'),
  })
  .refine((v) => v.reason !== 'OTHER' || v.note.length > 0, {
    path: ['note'],
    message: 'admin-system.common.err.reason',
  });
export type SuspendValues = z.infer<typeof suspendSchema>;

export const reactivateSchema = z.object({ reason });
export type ReactivateValues = z.infer<typeof reactivateSchema>;

export const nameSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(1, 'admin-system.account.err.name')
    .max(100, 'admin-system.account.err.name'),
});
export type NameValues = z.infer<typeof nameSchema>;

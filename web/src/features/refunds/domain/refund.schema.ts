// Tài khoản nhận tiền hoàn (đơn VietQR / COD) — thông điệp là khóa i18n.
import { z } from 'zod';

export const refundBankSchema = z.object({
  code: z.string().min(1, 'refunds.err.bank'),
  accountNumber: z.string().regex(/^\d{6,20}$/, 'refunds.err.account'),
  holder: z.string().trim().min(2, 'refunds.err.holder').max(100, 'refunds.err.holder'),
});

export type RefundBankValues = z.infer<typeof refundBankSchema>;

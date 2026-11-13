// Đọc / sửa nháp checkout dùng chung giữa trang Giỏ và trang Thanh toán (plan G10.21).
import type { CheckoutDraft } from '../domain/draft.types';
import { clearDraft, readDraft, writeDraft } from '../infrastructure/draft.storage';

export { readDraft as loadCheckoutDraft, clearDraft as clearCheckoutDraft };

/** Ghi đè một phần nháp, trả nháp mới. */
export function saveCheckoutDraft(patch: Partial<CheckoutDraft>): CheckoutDraft {
  const next = { ...readDraft(), ...patch };
  writeDraft(next);
  return next;
}

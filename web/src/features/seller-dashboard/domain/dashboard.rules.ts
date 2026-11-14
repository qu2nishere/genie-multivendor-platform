// Quy tắc Tổng quan: số giờ còn lại của yêu cầu trả hàng gần hạn nhất, tổng việc tồn.
import type { Dashboard } from './dashboard.types';

/** Số giờ (làm tròn lên, tối thiểu 0) tới hạn phản hồi trả hàng; null khi không có. */
export function hoursLeft(deadline: string | null, now: number): number | null {
  if (!deadline) return null;
  return Math.max(0, Math.ceil((Date.parse(deadline) - now) / 3_600_000));
}

export const backlog = (w: Dashboard['work']): number =>
  w.toConfirm + w.toPrepare + w.deliveryFailed + w.returns.count + w.unreadChats;

/** Tổng tiền của shop ở 4 ngăn (thanh "Tiền của shop"). */
export const moneyTotal = (m: Dashboard['money']): number =>
  m.available + m.held + m.pendingWithdrawal + m.codPending;

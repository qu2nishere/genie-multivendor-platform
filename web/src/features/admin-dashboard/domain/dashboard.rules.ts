// Hàm thuần của Tổng quan sàn (prototype Dashboard.jsx).
import { formatMonthShort } from '@/shared/lib/format';
import type { FinishedGroup, GmvRange, MonthKey, MovingGroup } from './dashboard.types';

/** Cùng bộ tông với shared/ui/dash (Tone) — domain không import UI. */
export type DashTone = 'held' | 'ok' | 'wait' | 'info' | 'ship' | 'bad';

export const GMV_RANGES: GmvRange[] = ['7d', 'weekly', '6m'];
export const GROWING_PAGE_SIZE = 5;

/** Màu từng nhóm đơn trên Pipeline (prototype MOVING / ENDED). */
export const MOVING_TONE: Record<MovingGroup, DashTone> = {
  AWAITING_PAYMENT: 'held',
  PAID: 'info',
  PREPARING: 'info',
  DELIVERING: 'ship',
  DELIVERED: 'ok',
};
export const FINISHED_TONE: Record<FinishedGroup, DashTone> = {
  COMPLETED: 'ok',
  CANCELLED: 'wait',
  RETURNED_REFUNDED: 'bad',
};

/** Lợi nhuận ròng = hoa hồng − voucher sàn/freeship − Điểm − phí cổng (API 11.2). */
export function netProfit(p: {
  commission: number;
  voucherCost: number;
  coinCost: number;
  gatewayFee: number;
}): number {
  return p.commission - p.voucherCost - p.coinCost - p.gatewayFee;
}

/** "2026-06" → "06/2026". */
export function monthLabel(m: MonthKey): string {
  const [y, mo] = m.split('-');
  return y && mo ? `${mo}/${y}` : m;
}

/** Kỳ liền trước trong danh sách kỳ (mới nhất đứng đầu); null nếu là kỳ cũ nhất. */
export function previousMonth(months: readonly MonthKey[], m: MonthKey): MonthKey | null {
  const i = months.indexOf(m);
  return i >= 0 && i + 1 < months.length ? (months[i + 1] ?? null) : null;
}

/**
 * Nhãn trục của điểm biểu đồ GMV:
 * - 6m: "2026-01" → "T1" (vi) / "Jan" (en)
 * - 7d: "2026-06-24" → "24/6"
 * - weekly: "2026-06-01/2026-06-07" → "1–7/6"
 */
export function gmvPointLabel(label: string, range: GmvRange, locale: 'vi' | 'en'): string {
  if (range === '6m') {
    const mo = Number(label.split('-')[1]);
    if (!mo) return label;
    return formatMonthShort(mo, { locale });
  }
  const day = (iso: string) => {
    const [, mo, d] = iso.split('-');
    return { d: Number(d), mo: Number(mo) };
  };
  if (range === '7d') {
    const { d, mo } = day(label);
    return d && mo ? `${d}/${mo}` : label;
  }
  const [a, b] = label.split('/');
  if (!a || !b) return label;
  const s = day(a);
  const e = day(b);
  return s.mo === e.mo ? `${s.d}–${e.d}/${e.mo}` : `${s.d}/${s.mo}–${e.d}/${e.mo}`;
}

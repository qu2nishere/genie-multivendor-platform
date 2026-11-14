// Quy tắc Điểm Genie — TS thuần (plan G10.56).
import type { CoinSummary } from './coins.types';

export const CHECKIN_DAYS = 7;

/** Trạng thái 7 ô điểm danh: đã nhận / hôm nay / sắp tới. */
export function checkInDays(c: CoinSummary['checkIn']): ('done' | 'today' | 'next')[] {
  return Array.from({ length: CHECKIN_DAYS }, (_, i) => {
    const day = i + 1;
    if (day <= c.streakDays) return 'done';
    if (!c.checkedInToday && day === c.streakDays + 1) return 'today';
    return 'next';
  });
}

/** Điểm thưởng ngày thứ `day` (1..7) của chuỗi. */
export const rewardOf = (c: CoinSummary['checkIn'], day: number): number =>
  c.rewards[(day - 1) % CHECKIN_DAYS] ?? 0;

/** Nhãn ngắn trên ô ("+100", "+1k"). */
export const rewardLabel = (n: number): string => (n >= 1000 ? `+${n / 1000}k` : `+${n}`);

export const canRedeem = (balance: number, cost: number): boolean => balance >= cost;

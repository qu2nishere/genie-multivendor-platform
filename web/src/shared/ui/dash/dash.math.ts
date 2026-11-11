// Hàm thuần của bộ thống kê (prototype dash.jsx) — plan G06.34 có test.

/** Phần trăm thay đổi có dấu; null khi không có kỳ trước để so. */
export function change(cur: number, prev: number | null | undefined): number | null {
  return prev ? ((cur - prev) / prev) * 100 : null;
}

/** Bước chia trục "đẹp" (1 / 2 / 2,5 / 5 / 10 × 10^k) cho khoảng [0, max] chia n vạch. */
export function niceStep(max: number, n: number): number {
  const raw = max / n;
  const mag = Math.pow(10, Math.floor(Math.log10(raw || 1)));
  const f = raw / mag;
  return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10) * mag;
}

/** Các vạch trục từ 0 tới đỉnh làm tròn theo niceStep. */
export function axisTicks(max: number, n = 4): { step: number; top: number; ticks: number[] } {
  const step = niceStep(Math.max(max, 1), n);
  const top = Math.ceil(Math.max(max, 1) / step) * step;
  const ticks: number[] = [];
  for (let t = 0; t <= top + step / 2; t += step) ticks.push(Number(t.toFixed(10)));
  return { step, top, ticks };
}

/** Tỉ lệ phần trăm của một phần trong tổng (tổng 0 → 0). */
export function shareOf(value: number, total: number): number {
  return total > 0 ? (value / total) * 100 : 0;
}

export type Tone = 'held' | 'ok' | 'wait' | 'info' | 'ship' | 'bad';

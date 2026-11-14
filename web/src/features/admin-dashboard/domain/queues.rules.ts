import type { AdminQueues, QueueKey } from './queues.types';

/** Các hàng chờ hiện thành ô "Việc cần xử lý" (prototype Dashboard.jsx), theo thứ tự hiển thị. */
export const JOB_KEYS = [
  'kycPending',
  'productsPending',
  'disputesOpen',
  'withdrawalsPending',
  'codRemittancesWaiting',
] as const satisfies readonly QueueKey[];

/** Tổng số việc đang chờ (không cộng "lệnh bị chặn" vì đã nằm trong lệnh rút chờ). */
export function totalJobs(q: AdminQueues | undefined): number {
  if (!q) return 0;
  return JOB_KEYS.reduce((n, k) => n + (q[k] ?? 0), 0);
}

/** Số trên huy hiệu: 0 / thiếu quyền → không hiện; quá 99 → "99+". */
export function badgeLabel(n: number | undefined): string | null {
  if (!n || n <= 0) return null;
  return n > 99 ? '99+' : String(n);
}

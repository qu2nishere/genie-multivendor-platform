// Hàm thuần duyệt sản phẩm (API_SPEC 11.12, 16.4).
import type {
  ModerationLabel,
  ModerationRow,
  ModerationStatus,
  ModerationTab,
  Moderator,
} from './moderation.types';

export const MODERATION_TABS: ModerationTab[] = ['PENDING', 'PUBLISHED', 'REJECTED'];

/** Mã lý do từ chối / khóa sản phẩm (16.4). */
export const PRODUCT_REJECT_REASONS = [
  'MISSING_SAFETY_CERT',
  'POLICY_VIOLATION',
  'BAD_IMAGE',
  'MISLEADING_DESCRIPTION',
  'UNREASONABLE_PRICE',
  'PROHIBITED_ITEM',
] as const;
export type ProductRejectReason = (typeof PRODUCT_REJECT_REASONS)[number];

const TAB_STATUS: Record<ModerationTab, ModerationStatus[]> = {
  PENDING: ['PENDING_APPROVAL', 'FLAGGED'],
  PUBLISHED: ['PUBLISHED'],
  REJECTED: ['REJECTED'],
};

export const inModerationTab = (s: ModerationStatus, tab: ModerationTab): boolean =>
  TAB_STATUS[tab].includes(s);

export const moderationLabel = (r: Pick<ModerationRow, 'status' | 'requeued'>): ModerationLabel =>
  r.status === 'PENDING_APPROVAL' && r.requeued ? 'REQUEUED' : r.status;

/** "AI nghi ngờ" (FLAGGED) lên đầu hàng chờ. */
export function sortQueue<T extends Pick<ModerationRow, 'status'>>(rows: T[]): T[] {
  return [...rows].sort((a, b) => Number(b.status === 'FLAGGED') - Number(a.status === 'FLAGGED'));
}

export function matchesQuery(
  r: Pick<ModerationRow, 'name' | 'shop' | 'productId'>,
  q: string,
): boolean {
  const n = q.trim().toLowerCase();
  return !n || `${r.name} ${r.shop.name} ${r.productId}`.toLowerCase().includes(n);
}

/** Thanh quyết định: Từ chối (trừ khi đã từ chối), Duyệt (trừ khi đã duyệt), Chờ duyệt lại (khi đã quyết). */
export function moderationActions(s: ModerationStatus): ('reject' | 'approve' | 'requeue')[] {
  const out: ('reject' | 'approve' | 'requeue')[] = [];
  if (s !== 'REJECTED') out.push('reject');
  if (s !== 'PUBLISHED') out.push('approve');
  if (!inModerationTab(s, 'PENDING')) out.push('requeue');
  return out;
}

/** Màu điểm rủi ro Gemini: ≥ 85 đỏ, ≥ 30 vàng, còn lại xanh (prototype). */
export function riskTone(score: number): 'danger' | 'warning' | 'success' {
  if (score >= 85) return 'danger';
  if (score >= 30) return 'warning';
  return 'success';
}

/** Chia đều hàng chờ cho nhân viên đang nhận việc (vòng tròn, theo thứ tự danh sách). */
export function redistribute<T extends Pick<ModerationRow, 'status' | 'assignee'>>(
  rows: T[],
  staff: Moderator[],
): { rows: T[]; reassigned: number } {
  const on = staff.filter((s) => s.accepting);
  if (on.length === 0) return { rows, reassigned: 0 };
  let i = 0;
  let reassigned = 0;
  const next = rows.map((r) => {
    if (!inModerationTab(r.status, 'PENDING')) return r;
    const m = on[i++ % on.length]!;
    if (r.assignee?.id !== m.staffId) reassigned += 1;
    return { ...r, assignee: { id: m.staffId, name: m.name } };
  });
  return { rows: next, reassigned };
}

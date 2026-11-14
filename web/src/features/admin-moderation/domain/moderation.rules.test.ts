import { describe, expect, it } from 'vitest';
import {
  inModerationTab,
  matchesQuery,
  moderationActions,
  moderationLabel,
  redistribute,
  riskTone,
  sortQueue,
} from './moderation.rules';
import { reportActions, sortReports } from './review.rules';

describe('moderation.rules', () => {
  it('tab Chờ duyệt gồm PENDING_APPROVAL + FLAGGED, FLAGGED lên đầu', () => {
    expect(inModerationTab('FLAGGED', 'PENDING')).toBe(true);
    expect(inModerationTab('PUBLISHED', 'PENDING')).toBe(false);
    const sorted = sortQueue([
      { status: 'PENDING_APPROVAL' as const },
      { status: 'FLAGGED' as const },
    ]);
    expect(sorted[0]?.status).toBe('FLAGGED');
  });
  it('nhãn "Chờ duyệt lại" khi đưa lại hàng chờ', () => {
    expect(moderationLabel({ status: 'PENDING_APPROVAL', requeued: true })).toBe('REQUEUED');
    expect(moderationLabel({ status: 'FLAGGED', requeued: true })).toBe('FLAGGED');
  });
  it('thanh quyết định theo trạng thái', () => {
    expect(moderationActions('FLAGGED')).toEqual(['reject', 'approve']);
    expect(moderationActions('PUBLISHED')).toEqual(['reject', 'requeue']);
    expect(moderationActions('REJECTED')).toEqual(['approve', 'requeue']);
  });
  it('điểm rủi ro', () => {
    expect(riskTone(91)).toBe('danger');
    expect(riskTone(62)).toBe('warning');
    expect(riskTone(12)).toBe('success');
  });
  it('tìm theo tên, nhà bán, mã', () => {
    const r = { name: 'Đèn bàn LED', shop: { id: 1, name: 'Home & Living' }, productId: 1284 };
    expect(matchesQuery(r, 'home')).toBe(true);
    expect(matchesQuery(r, '1284')).toBe(true);
    expect(matchesQuery(r, 'serum')).toBe(false);
  });
  it('phân phối lại chỉ cho người đang nhận việc', () => {
    const rows = [
      { status: 'PENDING_APPROVAL' as const, assignee: { id: 9, name: 'C' } },
      { status: 'FLAGGED' as const, assignee: null },
      { status: 'PUBLISHED' as const, assignee: null },
    ];
    const staff = [
      { staffId: 7, name: 'A', accepting: true },
      { staffId: 9, name: 'C', accepting: false },
    ];
    const res = redistribute(rows, staff);
    expect(res.rows.map((r) => r.assignee?.id ?? null)).toEqual([7, 7, null]);
    expect(res.reassigned).toBe(2);
    expect(redistribute(rows, [{ staffId: 9, name: 'C', accepting: false }]).reassigned).toBe(0);
  });
});

describe('review.rules', () => {
  it('tiêu cực mạnh lên đầu rồi theo số báo cáo', () => {
    const rows = [
      { code: 'a', sentiment: null, reportCount: 5 },
      { code: 'b', sentiment: { label: 'NEGATIVE' as const, score: 0.71 }, reportCount: 1 },
      { code: 'c', sentiment: { label: 'NEGATIVE' as const, score: 0.96 }, reportCount: 3 },
    ];
    expect(sortReports(rows).map((r) => r.code)).toEqual(['c', 'a', 'b']);
  });
  it('thao tác theo trạng thái báo cáo', () => {
    expect(reportActions('PENDING')).toEqual(['keep', 'hide']);
    expect(reportActions('HIDDEN')).toEqual(['unhide']);
  });
});

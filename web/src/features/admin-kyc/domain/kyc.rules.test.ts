import { describe, expect, it } from 'vitest';
import { inTab, kycActions, parseShopRate, riskOf, statusKey } from './kyc.rules';
import { rejectSchema, supplementSchema } from './kyc.schema';

describe('kyc.rules', () => {
  it('tab Chờ duyệt gom các trạng thái chờ', () => {
    expect(inTab('AI_PROCESSING', 'PENDING')).toBe(true);
    expect(inTab('MANUAL_REVIEW', 'PENDING')).toBe(true);
    expect(inTab('SUPPLEMENT_REQUESTED', 'PENDING')).toBe(false);
    expect(inTab('REJECTED', 'ALL')).toBe(true);
    expect(statusKey('AI_PROCESSING')).toBe('PENDING');
  });
  it('mức rủi ro theo điểm', () => {
    expect(riskOf(12)).toBe('LOW');
    expect(riskOf(30)).toBe('MEDIUM');
    expect(riskOf(69)).toBe('MEDIUM');
    expect(riskOf(70)).toBe('HIGH');
  });
  it('thao tác theo trạng thái', () => {
    expect(kycActions('PENDING')).toEqual(['reject', 'approve', 'supplement']);
    expect(kycActions('VERIFIED')).toEqual(['requeue']);
    expect(kycActions('SUPPLEMENT_REQUESTED')).toEqual(['requeue']);
  });
  it('hoa hồng riêng 0–50%, rỗng = theo ngành', () => {
    expect(parseShopRate('')).toEqual({ ok: true, value: null });
    expect(parseShopRate('3,5')).toEqual({ ok: true, value: 3.5 });
    expect(parseShopRate('51')).toEqual({ ok: false });
    expect(parseShopRate('abc')).toEqual({ ok: false });
  });
  it('form từ chối: Lý do khác bắt buộc ghi chú', () => {
    expect(rejectSchema.safeParse({ reason: 'OTHER', note: ' ' }).success).toBe(false);
    expect(rejectSchema.safeParse({ reason: 'OTHER', note: 'Sai tên' }).success).toBe(true);
    expect(rejectSchema.safeParse({ reason: 'BLURRY_DOCUMENT', note: '' }).success).toBe(true);
  });
  it('form bổ sung: ít nhất 1 giấy tờ', () => {
    expect(supplementSchema.safeParse({ documents: [], note: '' }).success).toBe(false);
    expect(supplementSchema.safeParse({ documents: ['ID_FRONT'], note: '' }).success).toBe(true);
  });
});

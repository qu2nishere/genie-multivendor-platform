import { describe, expect, it } from 'vitest';
import { productRejectSchema } from './moderation.schema';

describe('productRejectSchema (API 11.12 reject)', () => {
  it('nhận mã lý do hợp lệ, ghi chú được cắt khoảng trắng', () => {
    const r = productRejectSchema.safeParse({ reason: 'BAD_IMAGE', note: '  ảnh mờ  ' });
    expect(r.success && r.data.note).toBe('ảnh mờ');
  });
  it('từ chối mã lý do lạ', () => {
    expect(productRejectSchema.safeParse({ reason: 'KHONG_CO', note: '' }).success).toBe(false);
  });
  it('ghi chú tối đa 500 ký tự', () => {
    const r = productRejectSchema.safeParse({ reason: 'BAD_IMAGE', note: 'x'.repeat(501) });
    expect(r.success).toBe(false);
    if (!r.success)
      expect(r.error.issues[0].message).toBe('admin-moderation.product.err.note_long');
  });
});

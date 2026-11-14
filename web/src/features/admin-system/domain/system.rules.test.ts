import { describe, expect, it } from 'vitest';
import {
  actionTone,
  inUserTab,
  matchesUser,
  presetOf,
  rangeOf,
  staffActions,
  suspendUntil,
  titleOf,
  togglePerm,
} from './system.rules';
import { inviteSchema, suspendSchema } from './system.schema';

describe('nhân viên & phân quyền', () => {
  it('chức danh nạp bộ quyền mặc định, chỉnh khác → Tùy chỉnh', () => {
    const mod = presetOf('MODERATOR', []);
    expect(mod).toEqual(['VIEW_DASHBOARD', 'REVIEW_KYC', 'MANAGE_CATALOG', 'MODERATE_REVIEW']);
    expect(titleOf('MODERATOR', mod)).toBe('MODERATOR');
    expect(titleOf('MODERATOR', togglePerm(mod, 'VIEW_FINANCE'))).toBe('CUSTOM');
    expect(presetOf('CUSTOM', ['VIEW_FINANCE'])).toEqual(['VIEW_FINANCE']);
  });
  it('không cấp MANAGE_RBAC cho nhân viên, giữ thứ tự bảng quyền', () => {
    expect(togglePerm(['VIEW_DASHBOARD'], 'MANAGE_RBAC')).toEqual(['VIEW_DASHBOARD']);
    expect(togglePerm(['VIEW_FINANCE'], 'VIEW_DASHBOARD')).toEqual([
      'VIEW_DASHBOARD',
      'VIEW_FINANCE',
    ]);
  });
  it('thao tác theo trạng thái', () => {
    expect(staffActions({ role: 'ADMIN', status: 'ACTIVE', isSelf: true })).toEqual([]);
    expect(staffActions({ role: 'STAFF', status: 'PENDING_ACTIVATION', isSelf: false })).toEqual([
      'resend',
      'edit',
      'delete',
    ]);
    expect(staffActions({ role: 'STAFF', status: 'LOCKED', isSelf: false })).toEqual([
      'edit',
      'unlock',
      'delete',
    ]);
  });
  it('lời mời: email @genie.example', () => {
    const ok = {
      fullName: 'Hồ Thanh Trúc',
      email: 'Truc.HT@genie.example',
      staffTitle: 'CS_SPECIALIST' as const,
    };
    const r = inviteSchema.safeParse(ok);
    expect(r.success && r.data.email).toBe('truc.ht@genie.example');
    expect(inviteSchema.safeParse({ ...ok, email: 'truc@gmail.com' }).success).toBe(false);
  });
});

describe('người dùng', () => {
  const u = {
    fullName: 'Lê Hoàng Phúc',
    email: 'phuc.le@yahoo.com',
    phone: '0978112340',
    shopName: null,
    role: 'CUSTOMER' as const,
    status: 'SUSPENDED' as const,
  };
  it('tab + tìm theo SĐT có khoảng trắng', () => {
    expect(inUserTab(u, 'LOCKED')).toBe(true);
    expect(inUserTab(u, 'VENDOR')).toBe(false);
    expect(matchesUser(u, '0978 112 340')).toBe(true);
    expect(matchesUser(u, 'yahoo')).toBe(true);
    expect(matchesUser(u, 'techzone')).toBe(false);
  });
  it('hạn đình chỉ + lý do khác bắt buộc mô tả', () => {
    const now = new Date('2026-06-22T00:00:00Z');
    expect(suspendUntil('P7D', now)).toBe('2026-06-29T00:00:00.000Z');
    expect(suspendUntil('FOREVER', now)).toBeNull();
    expect(suspendSchema.safeParse({ reason: 'OTHER', duration: 'P7D', note: '' }).success).toBe(
      false,
    );
    expect(
      suspendSchema.safeParse({ reason: 'FRAUD', duration: 'FOREVER', note: '' }).success,
    ).toBe(true);
  });
});

describe('nhật ký', () => {
  it('khoảng thời gian theo giờ VN', () => {
    const now = new Date('2026-06-22T03:00:00Z');
    const c = { from: '', to: '' };
    expect(rangeOf('TODAY', now, c)).toEqual({ from: '2026-06-22', to: '2026-06-22' });
    expect(rangeOf('7D', now, c)).toEqual({ from: '2026-06-15', to: '2026-06-22' });
    expect(rangeOf('CUSTOM', now, { from: '2026-06-01', to: '' })).toEqual({
      from: '2026-06-01',
      to: null,
    });
  });
  it('màu hành động', () => {
    expect(actionTone('LOGIN_FAILED')).toBe('danger');
    expect(actionTone('KYC_APPROVED')).toBe('success');
    expect(actionTone('PAYOUT_APPROVED')).toBe('mint');
    expect(actionTone('REFUND_PROCESSED')).toBe('warning');
    expect(actionTone('STAFF_INVITED')).toBe('primary');
  });
});

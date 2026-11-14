import { describe, expect, it } from 'vitest';
import {
  can,
  canAny,
  isGrantable,
  PERMISSIONS,
  presetFor,
  segregationWarning,
  TITLE_PRESETS,
} from './permissions';

describe('permissions', () => {
  it('đủ 13 mã quyền', () => {
    expect(PERMISSIONS).toHaveLength(13);
    expect(new Set(PERMISSIONS).size).toBe(13);
  });

  it('preset theo chức danh khớp prototype ROLE_PERMS', () => {
    expect(presetFor('ADMIN')).toHaveLength(13);
    expect(presetFor('MODERATOR')).toEqual([
      'VIEW_DASHBOARD',
      'REVIEW_KYC',
      'MANAGE_CATALOG',
      'MODERATE_REVIEW',
    ]);
    expect(presetFor('CS_SPECIALIST')).toEqual([
      'VIEW_DASHBOARD',
      'MODERATE_REVIEW',
      'HANDLE_DISPUTE',
    ]);
    expect(presetFor('FINANCE_OFFICER')).toContain('PROCESS_PAYOUT');
    expect(presetFor('CUSTOM')).toEqual([]);
  });

  it('không preset nào của STAFF có MANAGE_RBAC', () => {
    for (const [title, perms] of Object.entries(TITLE_PRESETS)) {
      if (title !== 'ADMIN') expect(perms).not.toContain('MANAGE_RBAC');
    }
    expect(isGrantable('MANAGE_RBAC', 'STAFF')).toBe(false);
    expect(isGrantable('MANAGE_RBAC', 'ADMIN')).toBe(true);
    expect(isGrantable('VIEW_FINANCE', 'STAFF')).toBe(true);
  });

  it('can / canAny / cảnh báo tách nhiệm vụ', () => {
    const p = presetFor('FINANCE_OFFICER');
    expect(can(p, 'VIEW_FINANCE')).toBe(true);
    expect(can(p, 'REVIEW_KYC')).toBe(false);
    expect(can(undefined, 'VIEW_DASHBOARD')).toBe(false);
    expect(canAny(p, ['REVIEW_KYC', 'VIEW_FINANCE'])).toBe(true);
    expect(canAny(p, [])).toBe(true);
    expect(segregationWarning(p)).toBe(true);
    expect(segregationWarning(presetFor('MODERATOR'))).toBe(false);
  });
});

import { describe, expect, it } from 'vitest';
import { sectionOf } from './usePermissionUpdates';

describe('sectionOf', () => {
  it('finds the sidebar section of the current path', () => {
    expect(sectionOf('/')?.section).toBe('overview');
    expect(sectionOf('/users')?.section).toBe('users');
    expect(sectionOf('/kyc/12')?.section).toBe('kyc');
    expect(sectionOf('/khong-co')).toBeUndefined();
  });
});

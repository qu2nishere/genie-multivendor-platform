import { describe, expect, it } from 'vitest';
import { flashSlots, liveSlot, pickSlot, secondsUntilHour, slotLabel } from './flash.rules';

describe('flash.rules', () => {
  it('trạng thái các khung theo giờ', () => {
    expect(flashSlots(10).map((s) => s.state)).toEqual([
      'ended',
      'live',
      'upcoming',
      'upcoming',
      'upcoming',
    ]);
    expect(flashSlots(0).map((s) => s.state)).toEqual([
      'live',
      'upcoming',
      'upcoming',
      'upcoming',
      'upcoming',
    ]);
    expect(flashSlots(23).map((s) => s.state)).toEqual([
      'ended',
      'ended',
      'ended',
      'ended',
      'live',
    ]);
  });

  it('khung đang chạy và giờ kết thúc', () => {
    expect(liveSlot(13)).toEqual({ hour: 12, endHour: 15, state: 'live' });
    expect(liveSlot(21).endHour).toBe(24);
  });

  it('chọn khung từ query, bỏ khung đã qua / giá trị lạ', () => {
    expect(pickSlot(10, '15').hour).toBe(15);
    expect(pickSlot(10, '0').hour).toBe(9);
    expect(pickSlot(10, 'abc').hour).toBe(9);
    expect(pickSlot(10, null).hour).toBe(9);
  });

  it('đếm giây và nhãn giờ', () => {
    expect(secondsUntilHour(12, { hour: 11, minute: 59, second: 30 })).toBe(30);
    expect(secondsUntilHour(9, { hour: 10, minute: 0, second: 0 })).toBe(0);
    expect(slotLabel(9)).toBe('09:00');
  });
});

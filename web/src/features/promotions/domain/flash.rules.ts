// Khung giờ Flash Sale (DESIGN.md Flash Band; prototype chưa có màn riêng — plan G08.38).
// Mỗi ngày 5 khung theo giờ Việt Nam; khung đang chạy kết thúc khi khung sau bắt đầu (khung cuối: 24:00).

export const FLASH_SLOT_HOURS: readonly number[] = [0, 9, 12, 15, 20];

export type FlashSlotState = 'ended' | 'live' | 'upcoming';

export interface FlashSlot {
  hour: number;
  /** Giờ kết thúc (khung sau, hoặc 24). */
  endHour: number;
  state: FlashSlotState;
}

/** Các khung trong ngày theo giờ hiện tại (0–23, giờ Việt Nam). */
export function flashSlots(nowHour: number): FlashSlot[] {
  return FLASH_SLOT_HOURS.map((hour, i) => {
    const endHour = FLASH_SLOT_HOURS[i + 1] ?? 24;
    const state: FlashSlotState =
      nowHour >= endHour ? 'ended' : nowHour >= hour ? 'live' : 'upcoming';
    return { hour, endHour, state };
  });
}

/** Khung đang chạy (luôn có, vì khung đầu bắt đầu lúc 0 giờ). */
export function liveSlot(nowHour: number): FlashSlot {
  const all = flashSlots(nowHour);
  return all.find((s) => s.state === 'live') ?? all[all.length - 1];
}

/** Khung được chọn từ ?slot=HH; giá trị lạ hoặc khung đã qua → khung đang chạy. */
export function pickSlot(nowHour: number, param: string | null): FlashSlot {
  const all = flashSlots(nowHour);
  const h = param != null && /^\d{1,2}$/.test(param) ? Number(param) : NaN;
  const hit = all.find((s) => s.hour === h && s.state !== 'ended');
  return hit ?? liveSlot(nowHour);
}

/** Giây còn lại tới khi khung kết thúc / bắt đầu. */
export function secondsUntilHour(
  targetHour: number,
  now: { hour: number; minute: number; second: number },
): number {
  const nowS = now.hour * 3600 + now.minute * 60 + now.second;
  return Math.max(0, targetHour * 3600 - nowS);
}

export const slotLabel = (hour: number): string => `${String(hour).padStart(2, '0')}:00`;

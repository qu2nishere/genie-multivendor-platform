import type { Banner, BannerPosition, BannerState } from './banner.types';

/** Ngày địa phương YYYY-MM-DD (prototype localDay). */
export function localDay(d: Date = new Date()): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function bannerState(
  b: Pick<Banner, 'on' | 'startsAt' | 'endsAt'>,
  today: string,
): BannerState {
  if (!b.on) return 'OFF';
  if (b.endsAt && b.endsAt < today) return 'ENDED';
  if (b.startsAt && b.startsAt > today) return 'SCHEDULED';
  return 'LIVE';
}

/** Banner đang chạy của một vị trí (và danh mục nếu có), theo thứ tự Admin sắp. */
export function liveBanners(
  all: Banner[],
  position: BannerPosition,
  today: string,
  categorySlug?: string,
): Banner[] {
  return all
    .filter(
      (b) =>
        b.position === position &&
        bannerState(b, today) === 'LIVE' &&
        (!categorySlug || b.categorySlug === categorySlug),
    )
    .sort((a, b) => a.order - b.order);
}

/** Vị trí hai ô: chia xen kẽ 1-3-5… / 2-4-6… để mỗi ô tự xoay (prototype dealTwo). */
export function dealTwo<T>(items: T[]): T[][] {
  return [items.filter((_, k) => k % 2 === 0), items.filter((_, k) => k % 2 === 1)].filter(
    (s) => s.length > 0,
  );
}

/** Thời gian mỗi slide (ms). */
export const SLIDE_MS = 6000;

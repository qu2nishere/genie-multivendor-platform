// Banner công khai (GET /public/banners — API_SPEC 5.14) + đếm lượt xem / bấm gộp lô (POST /public/banner-events —
// API 5.15; plan 2026-11-16 banner B06.02–B06.03). Nối API thật khi PUBLIC_REAL_API có `banner`.
import { request } from '@/shared/api/http';
import { mockDelay } from '@/shared/api/mock';
import { env, isMocked } from '@/shared/config/env';
import { liveBanners, localDay } from '../domain/banner.rules';
import type { Banner, BannerPosition } from '../domain/banner.types';
import type { LiveBannerDto } from './banner.dto';
import { toBanner, toLiveBanner } from './banner.mapper';
import { BANNER_DTOS } from './banner.mock';

const BANNER_MOCK = isMocked('banner');

const baseUrl = () =>
  typeof window === 'undefined' ? (env.INTERNAL_API_URL ?? env.PUBLIC_API_URL) : env.PUBLIC_API_URL;

/**
 * Banner đang chạy của một vị trí. API thật lọc `CATEGORY_TOP` theo `categoryId` (bắt buộc với vị trí này); mock lọc theo
 * slug như prototype.
 */
export async function listLiveBanners(
  position: BannerPosition,
  categorySlug?: string,
  categoryId?: number,
): Promise<Banner[]> {
  if (BANNER_MOCK) {
    await mockDelay();
    return liveBanners(BANNER_DTOS.map(toBanner), position, localDay(), categorySlug);
  }
  if (position === 'CATEGORY_TOP' && categoryId === undefined) return [];
  const res = await request<Partial<Record<BannerPosition, LiveBannerDto[]>>>('/public/banners', {
    auth: false,
    baseUrl: baseUrl(),
    query: {
      positions: position,
      category_id: position === 'CATEGORY_TOP' ? categoryId : undefined,
    },
  });
  return (res.data[position] ?? []).map((d, i) =>
    toLiveBanner(d, position, i, categorySlug ?? null),
  );
}

// ---------- Đếm lượt xem / bấm: gom lô 5 giây, rời trang thì gửi bằng sendBeacon ----------

type BannerEvent = { banner_id: number; type: 'IMPRESSION' | 'CLICK' };

const FLUSH_MS = 5_000;
const MAX_BATCH = 50;
const queue: BannerEvent[] = [];
const seen = new Set<string>(); // mỗi banner chỉ đếm 1 lượt hiển thị / lần xem trang
let timer: ReturnType<typeof setTimeout> | null = null;
let listening = false;

const eventsUrl = () => `${env.PUBLIC_API_URL.replace(/\/$/, '')}/public/banner-events`;

function takeBatch(): BannerEvent[] {
  return queue.splice(0, MAX_BATCH);
}

async function flush(): Promise<void> {
  timer = null;
  while (queue.length > 0) {
    const events = takeBatch();
    try {
      await request('/public/banner-events', { method: 'POST', auth: false, body: { events } });
    } catch {
      /* số liệu chỉ để tham khảo CTR — bỏ qua lỗi mạng */
    }
  }
}

/** Rời trang / ẩn tab: gửi phần còn lại bằng sendBeacon (text/plain — server nhận cả kiểu này). */
function flushOnLeave(): void {
  if (typeof navigator === 'undefined' || typeof navigator.sendBeacon !== 'function') return;
  while (queue.length > 0) {
    const body = JSON.stringify({ events: takeBatch() });
    navigator.sendBeacon(eventsUrl(), new Blob([body], { type: 'text/plain' }));
  }
}

function enqueue(e: BannerEvent): void {
  queue.push(e);
  if (!listening && typeof window !== 'undefined') {
    listening = true;
    window.addEventListener('pagehide', flushOnLeave);
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') flushOnLeave();
    });
  }
  if (queue.length >= MAX_BATCH) void flush();
  else if (timer === null) timer = setTimeout(() => void flush(), FLUSH_MS);
}

/** Ghi nhận lượt xem / bấm (thống kê ở Admin → Banner). Không bao giờ ném lỗi, không chặn điều hướng. */
export async function trackBanner(id: string, event: 'view' | 'click'): Promise<void> {
  if (BANNER_MOCK || typeof window === 'undefined') return;
  const bannerId = Number(id);
  if (!Number.isFinite(bannerId)) return;
  if (event === 'view') {
    if (seen.has(id)) return;
    seen.add(id);
    enqueue({ banner_id: bannerId, type: 'IMPRESSION' });
  } else {
    enqueue({ banner_id: bannerId, type: 'CLICK' });
  }
}

/** Chỉ cho test: xóa hàng đợi và bộ nhớ "đã xem". */
export function resetBannerTrackingForTest(): void {
  queue.length = 0;
  seen.clear();
  if (timer !== null) clearTimeout(timer);
  timer = null;
}

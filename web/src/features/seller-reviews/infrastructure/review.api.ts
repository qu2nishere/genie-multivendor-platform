// Đánh giá phía shop (API_SPEC 10.8): GET /seller/reviews (+ summary), PUT /seller/reviews/{id}/reply.
// Báo cáo vi phạm dùng chung POST /reviews/{id}/reports (feature reviews). Mock: prototype REVIEWS, DIST.
import { request, api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { productImg } from '@/shared/lib/assets';
import { hasPhone, inTab } from '../domain/review.rules';
import type { ReviewStats, ReviewTab, ShopReview } from '../domain/review.types';

interface ReviewDto {
  id: number;
  reviewer: { display_name: string; avatar_url: string | null };
  rating: number;
  product_name: string;
  variant_name: string | null;
  content: string;
  media: { url: string; media_type: 'IMAGE' | 'VIDEO' }[];
  created_at: string;
  reply: { content: string; replied_at: string } | null;
  is_reported_by_me: boolean;
}

const toReview = (d: ReviewDto): ShopReview => ({
  id: d.id,
  reviewer: { name: d.reviewer.display_name, avatarUrl: d.reviewer.avatar_url },
  rating: d.rating,
  productName: d.product_name,
  variantName: d.variant_name,
  content: d.content,
  images: d.media.filter((m) => m.media_type === 'IMAGE').map((m) => m.url),
  createdAt: d.created_at,
  reply: d.reply && { content: d.reply.content, repliedAt: d.reply.replied_at },
  reportedByMe: d.is_reported_by_me,
});

const rv = (
  id: number,
  name: string,
  rating: number,
  product: string,
  variant: string,
  at: string,
  content: string,
  imgs: string[],
  reply: string | null,
): ReviewDto => ({
  id,
  reviewer: { display_name: name, avatar_url: null },
  rating,
  product_name: product,
  variant_name: variant,
  content,
  media: imgs.map((i) => ({ url: productImg(i), media_type: 'IMAGE' as const })),
  created_at: `${at}T10:00:00+07:00`,
  reply: reply ? { content: reply, replied_at: `${at}T12:00:00+07:00` } : null,
  is_reported_by_me: false,
});

const db = mockState('seller.reviews', () => [
  rv(
    5521,
    'Trần T. Mai',
    5,
    'Tai nghe ANC Pro 5',
    'Đen',
    '2026-06-08',
    'Âm thanh hay, chống ồn tốt, pin trâu. Shop đóng gói cẩn thận, giao nhanh!',
    ['headphones'],
    'Cảm ơn bạn đã ủng hộ shop 🧡',
  ),
  rv(
    5522,
    'Nguyễn V. An',
    4,
    'Watch S2 AMOLED',
    'Đen',
    '2026-06-07',
    'Đồng hồ đẹp, đo SpO2 ổn. Trừ 1 sao vì giao hơi chậm.',
    [],
    null,
  ),
  rv(
    5520,
    'Lê H. Phúc',
    2,
    'Bàn phím cơ RGB',
    'Blue switch',
    '2026-06-05',
    'Một phím bị kẹt, mong shop hỗ trợ đổi.',
    ['keyboard'],
    null,
  ),
]);
/** Tổng số trên toàn shop (prototype DIST) — danh sách mẫu chỉ là trang đầu. */
const DIST = { 5: 842, 4: 196, 3: 48, 2: 14, 1: 9 } as const;

export async function listShopReviews(
  tab: ReviewTab,
): Promise<{ items: ShopReview[]; stats: ReviewStats }> {
  if (USE_MOCK) {
    await mockDelay(200);
    const all = db.get().map(toReview);
    const count = Object.values(DIST).reduce((n, v) => n + v, 0);
    const sum = Object.entries(DIST).reduce((n, [s, v]) => n + Number(s) * v, 0);
    return {
      items: all.filter((r) => inTab(r, tab)),
      stats: {
        avg: Math.round((sum / count) * 10) / 10,
        count,
        distribution: { ...DIST },
        counts: {
          ALL: all.length,
          UNREPLIED: all.filter((r) => inTab(r, 'UNREPLIED')).length,
          LOW_RATING: all.filter((r) => inTab(r, 'LOW_RATING')).length,
        },
      },
    };
  }
  const res = await request<ReviewDto[]>('/seller/reviews', { query: { filter: tab, size: 50 } });
  const s = (res.summary ?? {}) as {
    rating_avg?: number;
    rating_count?: number;
    distribution?: Record<string, number>;
    counts?: Partial<Record<ReviewTab, number>>;
  };
  const d = s.distribution ?? {};
  return {
    items: res.data.map(toReview),
    stats: {
      avg: s.rating_avg ?? 0,
      count: s.rating_count ?? 0,
      distribution: {
        1: d['1'] ?? 0,
        2: d['2'] ?? 0,
        3: d['3'] ?? 0,
        4: d['4'] ?? 0,
        5: d['5'] ?? 0,
      },
      counts: {
        ALL: s.counts?.ALL ?? 0,
        UNREPLIED: s.counts?.UNREPLIED ?? 0,
        LOW_RATING: s.counts?.LOW_RATING ?? 0,
      },
    },
  };
}

export async function replyReview(id: number, content: string): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    if (hasPhone(content)) mockFail('GNE-SOC-4221');
    const all = db.get();
    if (all.find((r) => r.id === id)?.reply) mockFail('GNE-SOC-4094');
    db.set(
      all.map((r) =>
        r.id === id
          ? { ...r, reply: { content: content.trim(), replied_at: new Date().toISOString() } }
          : r,
      ),
    );
    return;
  }
  await api.put(`/seller/reviews/${id}/reply`, { content: content.trim() });
}

/** Đánh dấu đã báo cáo trong mock (API thật trả `is_reported_by_me` ở lần tải sau). */
export function markReported(id: number): void {
  if (!USE_MOCK) return;
  db.set(db.get().map((r) => (r.id === id ? { ...r, is_reported_by_me: true } : r)));
}

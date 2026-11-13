// Đánh giá mẫu (prototype ProductDetail.jsx REVIEWS) — mỗi sản phẩm dùng cùng 4 đánh giá như bản gốc.
// Trạng thái "Hữu ích" / đã báo cáo giữ trong bộ nhớ (mất khi tải lại server dev).
import { productImg } from '@/shared/lib/assets';
import type { ReviewDto } from './review.dto';

const BASE = [
  {
    key: 'r1',
    author_name: 'Nguyễn Minh',
    created_at: '2026-05-28',
    rating: 5,
    variant_label: 'Đen / Bản Pro',
    content:
      'Sản phẩm đúng mô tả, chất lượng tốt, đóng gói cẩn thận. Giao hàng nhanh trong 2 ngày. Sẽ ủng hộ shop lần sau!',
    images: ['headphones', 'banner-tech'],
    reply: 'Cảm ơn bạn đã tin tưởng TechZone! Shop rất vui khi sản phẩm làm bạn hài lòng 🧡',
    anon: false,
  },
  {
    key: 'r2',
    author_name: 'Trần Thu Hà',
    created_at: '2026-05-21',
    rating: 4,
    variant_label: 'Trắng / Tiêu chuẩn',
    content:
      'Pin trâu, âm thanh ổn trong tầm giá. Trừ 1 sao vì hộp hơi móp khi nhận nhưng sản phẩm vẫn nguyên vẹn.',
    images: [],
    reply: null,
    anon: false,
  },
  {
    key: 'r3',
    author_name: 'Lê Hoàng',
    created_at: '2026-05-15',
    rating: 5,
    variant_label: 'Đen / Tiêu chuẩn',
    content: 'Chống ồn tốt, đeo êm tai. Đáng tiền!',
    images: ['speaker'],
    reply: null,
    anon: true,
  },
  {
    key: 'r4',
    author_name: 'Phạm An',
    created_at: '2026-05-02',
    rating: 3,
    variant_label: 'Trắng / Tiêu chuẩn',
    content: 'Dùng tạm ổn, kết nối đôi lúc chập chờn. Shop hỗ trợ đổi hàng nhanh.',
    images: [] as string[],
    reply: null,
    anon: false,
  },
];

const helpful = new Map<string, boolean>();
export const reported = new Set<string>();

export function reviewDtos(productId: number): ReviewDto[] {
  return BASE.map((b) => {
    const id = `${productId}-${b.key}`;
    const mine = helpful.get(id) ?? false;
    return {
      id,
      product_id: productId,
      author_name: b.author_name,
      is_anonymous: b.anon,
      rating: b.rating,
      created_at: b.created_at,
      variant_label: b.variant_label,
      content: b.content,
      images: b.images.map((m) => productImg(m)),
      seller_reply: b.reply ? { content: b.reply } : null,
      // prototype: 12 + (name.length * 7) % 31
      helpful_count: 12 + ((b.author_name.length * 7) % 31) + (mine ? 1 : 0),
      helpful_by_me: mine,
      verified_purchase: true,
    };
  });
}

export function toggleMockHelpful(id: string): boolean {
  const next = !(helpful.get(id) ?? false);
  helpful.set(id, next);
  return next;
}

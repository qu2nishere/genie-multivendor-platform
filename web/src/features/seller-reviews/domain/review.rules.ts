// Quy tắc đánh giá phía shop: phản hồi ≤ 500 ký tự, không chứa số điện thoại (422 GNE-SOC-4221), lọc tab,
// phần trăm thanh phân bố sao.
import type { ReviewStats, ReviewTab, ShopReview } from './review.types';

export const REPLY_MAX = 500;
/** Số điện thoại Việt Nam trong phản hồi (0… hoặc +84…, có thể cách bằng khoảng trắng / . / -). */
export const hasPhone = (s: string): boolean => /(?:\+84|0)(?:[\s.-]?\d){8,10}/.test(s);

export type ReplyIssue = 'empty' | 'long' | 'phone';
export function replyIssue(s: string): ReplyIssue | null {
  const v = s.trim();
  if (!v) return 'empty';
  if (v.length > REPLY_MAX) return 'long';
  if (hasPhone(v)) return 'phone';
  return null;
}

export const inTab = (r: Pick<ShopReview, 'reply' | 'rating'>, tab: ReviewTab): boolean =>
  tab === 'ALL' || (tab === 'UNREPLIED' ? !r.reply : r.rating <= 3);

export const distPercent = (
  s: Pick<ReviewStats, 'distribution' | 'count'>,
  star: 1 | 2 | 3 | 4 | 5,
): number => (s.count > 0 ? (s.distribution[star] / s.count) * 100 : 0);

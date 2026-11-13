import { describe, expect, it } from 'vitest';
import { displayName, isReviewFilter, maskName, matchesFilter, summarize } from './review.rules';

const r = (rating: number, images: string[] = [], verifiedPurchase = true) => ({
  rating,
  images,
  verifiedPurchase,
});

describe('review.rules', () => {
  it('che tên đánh giá ẩn danh', () => {
    expect(maskName('Lê Hoàng')).toBe('L***g');
    expect(maskName('A')).toBe('A***');
    expect(displayName({ authorName: 'Phạm An', anonymous: false })).toBe('Phạm An');
    expect(displayName({ authorName: 'Phạm An', anonymous: true })).toBe('P***n');
  });

  it('lọc theo sao / có ảnh / đã mua', () => {
    expect(matchesFilter(r(5), '5')).toBe(true);
    expect(matchesFilter(r(4), '5')).toBe(false);
    expect(matchesFilter(r(3, ['a']), 'img')).toBe(true);
    expect(matchesFilter(r(3), 'img')).toBe(false);
    expect(matchesFilter(r(3, [], false), 'buy')).toBe(false);
    expect(matchesFilter(r(1), 'all')).toBe(true);
  });

  it('tổng hợp số lượng và điểm trung bình', () => {
    const s = summarize([r(5, ['x']), r(4), r(5), r(3)]);
    expect(s.total).toBe(4);
    expect(s.average).toBe(4.3);
    expect(s.counts).toEqual({ all: 4, '5': 2, '4': 1, '3': 1, img: 1, buy: 4 });
    expect(summarize([]).average).toBe(0);
  });

  it('isReviewFilter', () => {
    expect(isReviewFilter('img')).toBe(true);
    expect(isReviewFilter('2')).toBe(false);
    expect(isReviewFilter(null)).toBe(false);
  });
});

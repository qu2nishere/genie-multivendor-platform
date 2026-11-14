import { describe, expect, it } from 'vitest';
import { distPercent, hasPhone, inTab, replyIssue } from './review.rules';

describe('seller review rules', () => {
  it('phản hồi không chứa SĐT, ≤ 500 ký tự', () => {
    expect(hasPhone('Gọi shop 0901 234 567 nhé')).toBe(true);
    expect(hasPhone('Liên hệ +84.901.234.567')).toBe(true);
    expect(hasPhone('Cảm ơn bạn đã ủng hộ shop 🧡')).toBe(false);
    expect(replyIssue('   ')).toBe('empty');
    expect(replyIssue('x'.repeat(501))).toBe('long');
    expect(replyIssue('zalo 0912345678')).toBe('phone');
    expect(replyIssue('Shop sẽ đổi phím mới cho bạn ạ')).toBeNull();
  });
  it('tab + phân bố', () => {
    expect(inTab({ reply: null, rating: 5 }, 'UNREPLIED')).toBe(true);
    expect(inTab({ reply: null, rating: 4 }, 'LOW_RATING')).toBe(false);
    expect(inTab({ reply: { content: 'a', repliedAt: '' }, rating: 2 }, 'LOW_RATING')).toBe(true);
    expect(
      distPercent({ count: 1109, distribution: { 5: 842, 4: 196, 3: 48, 2: 14, 1: 9 } }, 5).toFixed(
        1,
      ),
    ).toBe('75.9');
  });
});

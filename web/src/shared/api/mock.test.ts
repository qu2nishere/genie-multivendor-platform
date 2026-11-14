import { describe, expect, it } from 'vitest';
import { ApiError } from './errors';
import { mockFail, mockOk, paginateCursor, paginatePage } from './mock';
import type { CursorPage } from './types';

const items = Array.from({ length: 45 }, (_, i) => i + 1);

describe('mock', () => {
  it('phân trang con trỏ đi hết danh sách, không trùng / sót', () => {
    const seen: number[] = [];
    let cursor: string | null = null;
    let guard = 0;
    do {
      const p: CursorPage<number> = paginateCursor(items, { limit: 20, cursor });
      seen.push(...p.items);
      cursor = p.next_cursor;
      expect(p.has_more).toBe(cursor !== null);
    } while (cursor && ++guard < 10);
    expect(seen).toEqual(items);
  });

  it('limit kẹp trong 1..100, con trỏ hỏng → về đầu', () => {
    expect(paginateCursor(items, { limit: 500 }).limit).toBe(100);
    expect(paginateCursor(items, { limit: 0 }).limit).toBe(1);
    expect(paginateCursor(items, { cursor: '@@@' }).items[0]).toBe(1);
  });

  it('phân trang số trang', () => {
    const p = paginatePage(items, { page: 3, size: 20 });
    expect(p.items).toEqual([41, 42, 43, 44, 45]);
    expect(p.total_pages).toBe(3);
    expect(p.total_items).toBe(45);
    expect(paginatePage(items, { page: 9 }).items).toEqual([]);
    expect(paginatePage([], {}).total_pages).toBe(1);
  });

  it('mockOk / mockFail đúng phong bì', () => {
    const ok = mockOk({ a: 1 });
    expect(ok.code).toBe('SUCCESS');
    expect(ok.data).toEqual({ a: 1 });
    try {
      mockFail('GNE-INV-4090');
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError);
      expect((e as ApiError).status).toBe(409);
    }
    expect(() => mockFail('GNE-AUTH-4101')).toThrowError(ApiError);
  });
});

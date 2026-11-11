import { describe, expect, it } from 'vitest';
import { pageList } from './Pagination';
import { $toasts, toast } from './toast.store';

describe('pageList (prototype Discover)', () => {
  it('chèn … đúng chỗ', () => {
    expect(pageList(1, 1)).toEqual([1]);
    expect(pageList(1, 3)).toEqual([1, 2, 3]);
    expect(pageList(2, 8)).toEqual([1, 2, 3, '…', 8]);
    expect(pageList(5, 8)).toEqual([1, '…', 4, 5, 6, '…', 8]);
    expect(pageList(8, 8)).toEqual([1, '…', 7, 8]);
  });
});

describe('toast store', () => {
  it('giữ tối đa 3 toast, bỏ cái cũ nhất, đóng theo id', () => {
    $toasts.set([]);
    const ids = ['a', 'b', 'c', 'd'].map((m) => toast.info(m, 0));
    expect($toasts.get().map((x) => x.message)).toEqual(['b', 'c', 'd']);
    toast.dismiss(ids[2]);
    expect($toasts.get().map((x) => x.message)).toEqual(['b', 'd']);
    toast.error('lỗi', 0);
    expect($toasts.get().at(-1)?.type).toBe('error');
  });
});

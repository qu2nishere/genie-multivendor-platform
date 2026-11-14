import { describe, expect, it } from 'vitest';
import {
  canDeleteRoot,
  duplicateCode,
  inProductTab,
  parentOptions,
  parseValues,
  productActions,
} from './catalog.rules';
import { attributeSchema, categorySchema } from './catalog.schema';
import type { RootCategory } from './catalog.types';

const root = (over: Partial<RootCategory> = {}): RootCategory => ({
  id: 1,
  name: 'Điện tử',
  slug: 'dien-tu',
  icon: 'zap',
  active: true,
  commissionRate: 5,
  productCount: 0,
  shopCount: 0,
  children: [],
  ...over,
});

const attr = {
  code: 'warranty',
  name: 'Bảo hành',
  dataType: 'ENUM' as const,
  valuesText: '6 tháng, 12 tháng',
  unit: '',
  required: true,
  filterable: true,
  variantAxis: false,
};

describe('catalog.rules', () => {
  it('tách danh sách giá trị, bỏ trống và trùng', () => {
    expect(parseValues(' 6 tháng, ,12 tháng,6 tháng ')).toEqual(['6 tháng', '12 tháng']);
  });
  it('chỉ xóa danh mục rỗng', () => {
    expect(canDeleteRoot(root())).toBe(true);
    expect(canDeleteRoot(root({ productCount: 3 }))).toBe(false);
    expect(
      canDeleteRoot(
        root({
          children: [{ id: 11, name: 'x', commissionRate: 5, productCount: 0, children: [] }],
        }),
      ),
    ).toBe(false);
  });
  it('danh mục cha gồm gốc và cấp 2', () => {
    const opts = parentOptions([
      root({
        children: [{ id: 11, name: 'Laptop', commissionRate: 5, productCount: 0, children: [] }],
      }),
    ]);
    expect(opts).toEqual([
      { id: 1, label: 'Điện tử', depth: 1 },
      { id: 11, label: 'Điện tử › Laptop', depth: 2 },
    ]);
  });
  it('tab và thao tác sản phẩm', () => {
    expect(inProductTab({ status: 'REJECTED' }, 'HIDDEN_OR_LOCKED')).toBe(true);
    expect(inProductTab({ status: 'PENDING_APPROVAL' }, 'PUBLISHED')).toBe(false);
    expect(productActions({ status: 'PENDING_APPROVAL', locked: false })).toEqual([
      'reject',
      'approve',
    ]);
    expect(productActions({ status: 'PUBLISHED', locked: false })).toEqual(['lock']);
    expect(productActions({ status: 'HIDDEN', locked: true })).toEqual(['unlock']);
    expect(productActions({ status: 'REJECTED', locked: false })).toEqual([]);
  });
  it('trùng mã thuộc tính', () => {
    expect(duplicateCode([{ code: 'color' }], 'color', null)).toBe(true);
    expect(duplicateCode([{ code: 'color' }], 'color', 'color')).toBe(false);
  });
  it('lược đồ thuộc tính: mã đúng mẫu, ENUM cần giá trị', () => {
    expect(attributeSchema.safeParse(attr).success).toBe(true);
    expect(attributeSchema.safeParse({ ...attr, code: '1abc' }).success).toBe(false);
    expect(attributeSchema.safeParse({ ...attr, valuesText: ' , ' }).success).toBe(false);
    expect(attributeSchema.safeParse({ ...attr, dataType: 'TEXT', valuesText: '' }).success).toBe(
      true,
    );
  });
  it('lược đồ danh mục: slug + hoa hồng 0–50', () => {
    const ok = {
      name: 'Thú cưng',
      slug: 'thu-cung',
      parentId: null,
      icon: 'heart',
      commissionRate: 5,
      active: true,
    };
    expect(categorySchema.safeParse(ok).success).toBe(true);
    expect(categorySchema.safeParse({ ...ok, slug: 'Thú cưng' }).success).toBe(false);
    expect(categorySchema.safeParse({ ...ok, commissionRate: 60 }).success).toBe(false);
  });
});

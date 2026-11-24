import { describe, expect, it } from 'vitest';
import {
  canDeleteCategory,
  duplicateCode,
  findCategory,
  flattenTree,
  inProductTab,
  parentOptions,
  parseValues,
  productActions,
} from './catalog.rules';
import { attributeSchema, categorySchema } from './catalog.schema';
import type { CategoryBase, RootCategory, SubCategory } from './catalog.types';

const root = (over: Partial<RootCategory> = {}): RootCategory => ({
  id: 1,
  name: 'Điện tử',
  slug: 'dien-tu',
  icon: 'zap',
  active: true,
  requiresLicense: false,
  commissionRate: 5,
  ownCommission: true,
  productCount: 0,
  shopCount: 0,
  children: [],
  ...over,
});

const base = (id: number, name: string, over: Partial<CategoryBase> = {}): CategoryBase => ({
  id,
  name,
  slug: name.toLowerCase(),
  icon: 'package',
  active: true,
  requiresLicense: false,
  commissionRate: 5,
  ownCommission: false,
  productCount: 0,
  ...over,
});
const sub = (id: number, name: string, over: Partial<SubCategory> = {}): SubCategory => ({
  ...base(id, name),
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
  it('chỉ xóa danh mục rỗng — ở mọi cấp', () => {
    const tree = [
      root({
        children: [
          sub(11, 'Laptop', { children: [base(111, 'Gaming')] }),
          sub(12, 'Tai nghe', { productCount: 4 }),
          sub(13, 'Rỗng'),
        ],
      }),
      root({ id: 2, name: 'Sách', slug: 'sach' }),
    ];
    const can = (id: number) => canDeleteCategory(findCategory(tree, id)!);
    expect(can(1)).toBe(false); // còn ngành con
    expect(can(2)).toBe(true);
    expect(can(11)).toBe(false); // còn cấp 3
    expect(can(12)).toBe(false); // còn sản phẩm
    expect(can(13)).toBe(true);
    expect(can(111)).toBe(true);
  });
  it('dàn phẳng cây 3 cấp, tìm ngành ở mọi cấp kèm vị trí', () => {
    const tree = [root({ children: [sub(11, 'Laptop', { children: [base(111, 'Gaming')] })] })];
    expect(
      flattenTree(tree).map((c) => [c.id, c.level, c.parentId, c.rootId, c.childCount]),
    ).toEqual([
      [1, 1, null, 1, 1],
      [11, 2, 1, 1, 1],
      [111, 3, 11, 1, 0],
    ]);
    expect(findCategory(tree, 111)).toMatchObject({ name: 'Gaming', level: 3, rootId: 1 });
    expect(findCategory(tree, 999)).toBeNull();
  });
  it('danh mục cha gồm gốc và cấp 2', () => {
    const opts = parentOptions([
      root({
        children: [sub(11, 'Laptop')],
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
  it('lược đồ danh mục: slug + hoa hồng 3,5–8', () => {
    const ok = {
      name: 'Thú cưng',
      slug: 'thu-cung',
      parentId: null,
      icon: 'heart',
      commissionRate: 5,
      active: true,
      requiresLicense: false,
    };
    expect(categorySchema.safeParse(ok).success).toBe(true);
    expect(categorySchema.safeParse({ ...ok, slug: 'Thú cưng' }).success).toBe(false);
    expect(categorySchema.safeParse({ ...ok, commissionRate: 8.5 }).success).toBe(false);
    expect(categorySchema.safeParse({ ...ok, commissionRate: 3 }).success).toBe(false);
    expect(categorySchema.safeParse({ ...ok, commissionRate: 3.5 }).success).toBe(true);
  });
});

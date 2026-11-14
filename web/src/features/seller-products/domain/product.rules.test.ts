import { describe, expect, it } from 'vitest';
import {
  combos,
  emptyForm,
  missingSpecs,
  publishIssues,
  requiresReview,
  skuPreview,
  suggestedSpecs,
  syncVariants,
  textLength,
  zeroVariants,
} from './product.rules';
import { basicsSchema, proposalSchema } from './product.schema';
import type { ListingRules, OptionGroup, ProductForm } from './product.types';

const g = (name: string, values: string[]): OptionGroup => ({
  name,
  values: values.map((value) => ({ value, imageKey: null, imageUrl: null })),
});
const rules: ListingRules = {
  categoryId: 111,
  commissionRate: 4,
  requiresDocuments: true,
  attributes: [
    { name: 'Bảo hành', required: true, values: [] },
    { name: 'Kết nối', required: false, values: [] },
  ],
};

describe('ma trận phân loại', () => {
  it('tổ hợp 2 nhóm Màu × Switch như bản mẫu', () => {
    expect(combos([g('Màu sắc', ['Đen', 'Trắng']), g('Switch', ['Blue', 'Red'])])).toEqual([
      ['Đen', 'Blue'],
      ['Đen', 'Red'],
      ['Trắng', 'Blue'],
      ['Trắng', 'Red'],
    ]);
    expect(combos([])).toEqual([[]]);
    expect(combos([g('Màu sắc', [])])).toEqual([[]]);
  });

  it('giữ giá / tồn khi thêm giá trị; mang giá đơn lẻ sang dòng mới', () => {
    const v1 = syncVariants(
      [g('Màu sắc', ['Đen'])],
      [{ id: null, options: [], sku: null, price: 100, stock: 5 }],
    );
    expect(v1).toEqual([{ id: null, options: ['Đen'], sku: null, price: 100, stock: 5 }]);
    const v2 = syncVariants([g('Màu sắc', ['Đen', 'Trắng'])], [{ ...v1[0], id: 9, price: 120 }]);
    expect(v2[0]).toMatchObject({ id: 9, price: 120 });
    expect(v2[1]).toMatchObject({ id: null, options: ['Trắng'], price: null });
  });

  it('SKU xem trước + đếm dòng giá 0', () => {
    expect(skuPreview(['Đen', 'Blue'])).toBe('SKU·ĐE·BL');
    expect(
      zeroVariants([
        { id: null, options: ['a'], sku: null, price: null, stock: null },
        { id: null, options: ['b'], sku: null, price: 0, stock: 0 },
        { id: null, options: ['c'], sku: null, price: 10, stock: 0 },
      ]),
    ).toBe(2);
  });
});

describe('thông số + điều kiện gửi duyệt', () => {
  it('thông số bắt buộc / gợi ý', () => {
    expect(missingSpecs([{ name: 'Bảo hành', value: ' ' }], rules)).toEqual(['Bảo hành']);
    expect(missingSpecs([{ name: 'Bảo hành', value: '12 tháng' }], rules)).toEqual([]);
    expect(suggestedSpecs([{ name: 'Bảo hành', value: '' }], rules)).toEqual(['Kết nối']);
  });

  it('form trống thiếu ảnh, tên, danh mục, giấy tờ, thông số, giá, cân nặng', () => {
    expect(publishIssues(emptyForm(), rules)).toEqual([
      'images',
      'name',
      'category',
      'documents',
      'specs',
      'price',
      'weight',
    ]);
  });

  it('độ dài mô tả không tính thẻ (xuống dòng tính 1 ký tự như innerText)', () => {
    expect(textLength('<p><b>Bàn</b> phím</p><ul><li>A&amp;B</li></ul>')).toBe(12);
  });

  it('chỉ đổi giá / tồn thì không cần duyệt lại', () => {
    const a: ProductForm = { ...emptyForm(), name: 'Bàn phím cơ' };
    const b: ProductForm = {
      ...a,
      variants: [{ id: 1, options: [], sku: null, price: 1, stock: 1 }],
    };
    expect(requiresReview('PUBLISHED', a, b)).toBe(false);
    expect(requiresReview('PUBLISHED', a, { ...a, name: 'Bàn phím cơ RGB' })).toBe(true);
    expect(requiresReview('DRAFT', a, { ...a, name: 'Khác' })).toBe(false);
  });
});

describe('lược đồ', () => {
  const base = {
    name: 'Bàn phím cơ RGB',
    origin: 'Việt Nam',
    condition: 'NEW' as const,
    weightGrams: 850,
    length: 30,
    width: 20,
    height: 10,
    shippingPayer: 'BUYER' as const,
    freeShippingThreshold: null,
    isPreOrder: false,
    preOrderDays: null,
  };
  it('đặt trước 7–30 ngày; freeship cần mức đơn', () => {
    expect(basicsSchema.safeParse(base).success).toBe(true);
    expect(basicsSchema.safeParse({ ...base, isPreOrder: true, preOrderDays: 5 }).success).toBe(
      false,
    );
    expect(basicsSchema.safeParse({ ...base, isPreOrder: true, preOrderDays: 14 }).success).toBe(
      true,
    );
    expect(basicsSchema.safeParse({ ...base, shippingPayer: 'SELLER' }).success).toBe(false);
  });
  it('đề xuất danh mục cần lý do', () => {
    expect(proposalSchema.safeParse({ name: 'Đồng hồ', parentId: 1, reason: '' }).success).toBe(
      false,
    );
  });
});

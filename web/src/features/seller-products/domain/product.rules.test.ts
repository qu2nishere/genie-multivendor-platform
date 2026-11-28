import { describe, expect, it } from 'vitest';
import {
  applyBulk,
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

describe('áp dụng hàng loạt theo giá trị', () => {
  const rows = [
    { id: null, options: ['128 GB', 'Đỏ'], sku: null, price: null, stock: null },
    { id: null, options: ['128 GB', 'Vàng'], sku: null, price: null, stock: 3 },
    { id: null, options: ['512 GB', 'Đỏ'], sku: null, price: null, stock: null },
  ];
  it('giá theo dung lượng: mọi màu của 128 GB cùng giá, tồn giữ nguyên', () => {
    const r = applyBulk(rows, ['128 GB', null], { price: 20_000_000, stock: null });
    expect(r.count).toBe(2);
    expect(r.variants.map((v) => v.price)).toEqual([20_000_000, 20_000_000, null]);
    expect(r.variants[1].stock).toBe(3);
  });
  it('không lọc = mọi phân loại', () => {
    expect(applyBulk(rows, [null, null], { price: null, stock: 5 }).count).toBe(3);
  });
  it('lọc theo cả hai nhóm', () => {
    const r = applyBulk(rows, ['512 GB', 'Đỏ'], { price: 30_000_000, stock: 1 });
    expect(r.count).toBe(1);
    expect(r.variants[2]).toMatchObject({ price: 30_000_000, stock: 1 });
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

  it('ngành lá cấp 2 (cây thật) là đủ danh mục', () => {
    const f: ProductForm = {
      ...emptyForm(),
      categoryPath: [
        { id: 1, name: 'Phụ kiện' },
        { id: 12, name: 'Đồng hồ & Trang sức' },
      ],
    };
    expect(publishIssues(f, rules)).not.toContain('category');
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
    isPreOrder: false,
    preOrderDays: null,
  };
  it('đặt trước 7–30 ngày; shop trả phí không cần mức đơn', () => {
    expect(basicsSchema.safeParse(base).success).toBe(true);
    expect(basicsSchema.safeParse({ ...base, isPreOrder: true, preOrderDays: 5 }).success).toBe(
      false,
    );
    expect(basicsSchema.safeParse({ ...base, isPreOrder: true, preOrderDays: 14 }).success).toBe(
      true,
    );
    expect(basicsSchema.safeParse({ ...base, shippingPayer: 'SELLER' }).success).toBe(true);
  });
  it('đề xuất danh mục cần lý do', () => {
    expect(proposalSchema.safeParse({ name: 'Đồng hồ', parentId: 1, reason: '' }).success).toBe(
      false,
    );
  });
});

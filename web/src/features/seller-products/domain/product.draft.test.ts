import { describe, expect, it } from 'vitest';
import {
  DRAFT_SCHEMA,
  DRAFT_TTL_MS,
  draftKey,
  draftUsable,
  dropExpiredUploads,
  hasContent,
  TMP_UPLOAD_TTL_MS,
  type ProductDraft,
} from './product.draft';
import { emptyForm } from './product.rules';
import type { ProductForm } from './product.types';

const NOW = 1_800_000_000_000;
const filled = (): ProductForm => ({
  ...emptyForm(),
  name: 'Tai nghe ANC',
  images: [
    { objectKey: 'tmp/user/7/a.webp', url: 'data:image/jpeg;base64,x' },
    { objectKey: 'vendor/7/product/9/b.webp', url: 'https://cdn/b.webp' },
  ],
  video: { objectKey: 'tmp/user/7/v.mp4', name: 'v.mp4' },
  documents: [{ objectKey: 'tmp/user/7/d.pdf', name: 'd.pdf' }],
  groups: [
    {
      name: 'Màu sắc',
      values: [
        { value: 'Đen', imageKey: 'tmp/user/7/den.webp', imageUrl: 'data:x' },
        { value: 'Bạc', imageKey: null, imageUrl: null },
      ],
    },
  ],
});
const draft = (over: Partial<ProductDraft> = {}): ProductDraft => ({
  schema: DRAFT_SCHEMA,
  savedAt: NOW - 1000,
  productVersion: null,
  form: filled(),
  ...over,
});

describe('bản nhập dở form đăng bán', () => {
  it('khóa theo người dùng + sản phẩm', () => {
    expect(draftKey(7, null)).toBe('seller.product_draft.7.new');
    expect(draftKey(7, 42)).toBe('seller.product_draft.7.42');
  });

  it('form trống không đáng giữ', () => {
    expect(hasContent(emptyForm())).toBe(false);
    expect(hasContent({ ...emptyForm(), descriptionHtml: '<p><br></p>' })).toBe(false);
    expect(hasContent({ ...emptyForm(), name: 'A' })).toBe(true);
  });

  it('chỉ gợi ý khi còn hạn, đúng cấu trúc, đúng version sản phẩm', () => {
    expect(draftUsable(draft(), NOW, null)).toBe(true);
    expect(draftUsable(null, NOW, null)).toBe(false);
    expect(draftUsable(draft({ schema: 0 }), NOW, null)).toBe(false);
    expect(draftUsable(draft({ savedAt: NOW - DRAFT_TTL_MS - 1 }), NOW, null)).toBe(false);
    expect(draftUsable(draft({ productVersion: 3 }), NOW, 3)).toBe(true);
    expect(draftUsable(draft({ productVersion: 3 }), NOW, 4)).toBe(false);
    expect(draftUsable(draft({ form: emptyForm() }), NOW, null)).toBe(false);
  });

  it('trong hạn kho thì giữ nguyên tệp tạm', () => {
    const r = dropExpiredUploads(filled(), NOW - TMP_UPLOAD_TTL_MS + 1, NOW);
    expect(r.dropped).toBe(false);
    expect(r.form.images).toHaveLength(2);
  });

  it('quá hạn kho thì bỏ mọi tệp tmp/, giữ tệp đã thuộc sản phẩm và chữ', () => {
    const r = dropExpiredUploads(filled(), NOW - TMP_UPLOAD_TTL_MS - 1, NOW);
    expect(r.dropped).toBe(true);
    expect(r.form.images.map((i) => i.objectKey)).toEqual(['vendor/7/product/9/b.webp']);
    expect(r.form.video).toBeNull();
    expect(r.form.documents).toHaveLength(0);
    expect(r.form.groups[0].values[0]).toEqual({ value: 'Đen', imageKey: null, imageUrl: null });
    expect(r.form.name).toBe('Tai nghe ANC');
  });
});

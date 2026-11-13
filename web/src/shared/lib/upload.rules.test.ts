import { describe, expect, it } from 'vitest';
import { compressionOf, isImage, isVideo, maxMb, uploadIssue } from './upload.rules';

describe('upload.rules', () => {
  it('kiểm MIME và kích thước theo mục đích', () => {
    expect(uploadIssue({ type: 'image/jpeg', size: 1000 }, 'REVIEW_IMAGE')).toBeNull();
    expect(uploadIssue({ type: 'image/gif', size: 1000 }, 'REVIEW_IMAGE')).toBe('type');
    expect(uploadIssue({ type: 'video/mp4', size: 11 * 1024 * 1024 }, 'REFUND_EVIDENCE')).toBe(
      'size',
    );
    expect(uploadIssue({ type: 'video/mp4', size: 29 * 1024 * 1024 }, 'REVIEW_VIDEO')).toBeNull();
    expect(maxMb('REFUND_EVIDENCE')).toBe(10);
  });
  it('loại tệp', () => {
    expect(isImage({ type: 'image/png' })).toBe(true);
    expect(isVideo({ type: 'video/mp4' })).toBe(true);
  });
  it('giấy tờ sản phẩm không nhận ảnh; nén theo mục đích', () => {
    expect(uploadIssue({ type: 'image/png', size: 1000 }, 'PRODUCT_DOCUMENT')).toBe('type');
    expect(uploadIssue({ type: 'application/pdf', size: 1000 }, 'PRODUCT_DOCUMENT')).toBeNull();
    expect(uploadIssue({ type: 'image/webp', size: 1000 }, 'PRODUCT_IMAGE')).toBeNull();
    expect(compressionOf('PRODUCT_IMAGE')).toBe('webp');
    expect(compressionOf('KYC_DOCUMENT')).toBeNull();
    expect(compressionOf('REVIEW_IMAGE')).toBe('jpeg');
  });
});

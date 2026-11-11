import { describe, expect, it } from 'vitest';
import { createT } from '@/shared/lib/i18n';
import { ApiError, ERROR_CODES, errorKey, errorMessage, normalizeCode, toApiError } from './errors';
import { hasKey } from '@/shared/lib/i18n';

describe('errors', () => {
  it('đổi mã prototype LZD-* sang GNE-*', () => {
    expect(normalizeCode('LZD-INV-4090')).toBe('GNE-INV-4090');
    expect(new ApiError({ code: 'LZD-INV-4090', status: 409, message: 'x' }).code).toBe(
      'GNE-INV-4090',
    );
  });

  it('mọi mã trong danh mục đều có khóa i18n ở cả vi và en', () => {
    for (const c of ERROR_CODES) {
      expect(hasKey(`errors.${c}`, 'vi'), c).toBe(true);
      expect(hasKey(`errors.${c}`, 'en'), c).toBe(true);
    }
  });

  it('mã lạ → errors.unknown; lỗi mạng / timeout có khóa riêng', () => {
    expect(errorKey('GNE-XYZ-4999')).toBe('errors.unknown');
    expect(errorKey(toApiError(new TypeError('Failed to fetch')).code)).toBe('errors.network');
    expect(errorKey(toApiError(new DOMException('aborted', 'AbortError')).code)).toBe(
      'errors.timeout',
    );
  });

  it('thông điệp nội suy details (số lần thử còn lại)', () => {
    const e = new ApiError({
      code: 'GNE-AUTH-4222',
      status: 422,
      message: '',
      details: { remaining_attempts: 3 },
    });
    expect(errorMessage(e, createT('vi'))).toBe('Mã OTP không đúng. Còn 3 lần thử.');
    expect(errorMessage(e, createT('en'))).toBe('Incorrect code. 3 attempts left.');
  });

  it('lỗi từng trường → map cho form', () => {
    const e = new ApiError({
      code: 'GNE-VAL-4220',
      status: 422,
      message: '',
      errors: [
        {
          field: 'phone_number',
          code: 'PATTERN_MISMATCH',
          message: 'Số điện thoại phải gồm 10 chữ số.',
        },
      ],
    });
    expect(e.fieldMap()).toEqual({ phone_number: 'Số điện thoại phải gồm 10 chữ số.' });
  });
});

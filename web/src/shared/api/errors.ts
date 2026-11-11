// Lỗi API → thông điệp i18n (plan G05.02; TS mục 8, Document_Vi 3.7.1).
// Mã prototype `LZD-*` được đổi sang `GNE-*` (R11), ví dụ LZD-INV-4090 → GNE-INV-4090.
import { hasKey, t as translate, type TParams } from '@/shared/lib/i18n';
import type { ApiErrorBody, ApiFieldError } from './types';
import { ERROR_CODES, type ErrorCode } from './error-codes';

export { ERROR_CODES, type ErrorCode };

/** Mã nội bộ phía client (không đến từ server). */
export const CLIENT_ERROR = {
  network: 'CLIENT-NETWORK',
  timeout: 'CLIENT-TIMEOUT',
  unknown: 'CLIENT-UNKNOWN',
} as const;

export class ApiError extends Error {
  readonly code: string;
  readonly status: number;
  readonly fieldErrors: ApiFieldError[];
  readonly details: Record<string, unknown>;
  readonly requestId?: string;

  constructor(body: ApiErrorBody) {
    super(body.message || body.code);
    this.name = 'ApiError';
    this.code = normalizeCode(body.code);
    this.status = body.status;
    this.fieldErrors = body.errors ?? [];
    this.details = body.details ?? {};
    this.requestId = body.meta?.request_id;
  }

  /** Lỗi theo từng trường → { field: message } để đổ vào react-hook-form. */
  fieldMap(): Record<string, string> {
    return Object.fromEntries(this.fieldErrors.map((e) => [e.field, e.message]));
  }
}

export const isApiError = (e: unknown): e is ApiError => e instanceof ApiError;

/** Đổi mã cũ của prototype sang tiền tố GNE-. */
export function normalizeCode(code: string): string {
  return code.replace(/^LZD-/, 'GNE-');
}

export const isKnownCode = (code: string): code is ErrorCode =>
  (ERROR_CODES as readonly string[]).includes(code);

/** Khóa i18n của một mã lỗi; mã lạ → errors.unknown. */
export function errorKey(code: string): string {
  const c = normalizeCode(code);
  if (c === CLIENT_ERROR.network) return 'errors.network';
  if (c === CLIENT_ERROR.timeout) return 'errors.timeout';
  const key = `errors.${c}`;
  return hasKey(key) ? key : 'errors.unknown';
}

/** Bọc mọi giá trị ném ra thành ApiError (lỗi mạng, hủy, lỗi lạ). */
export function toApiError(e: unknown): ApiError {
  if (e instanceof ApiError) return e;
  if (e instanceof DOMException && e.name === 'AbortError') {
    return new ApiError({ code: CLIENT_ERROR.timeout, status: 0, message: 'timeout' });
  }
  if (e instanceof TypeError) {
    return new ApiError({ code: CLIENT_ERROR.network, status: 0, message: e.message });
  }
  return new ApiError({
    code: CLIENT_ERROR.unknown,
    status: 0,
    message: e instanceof Error ? e.message : String(e),
  });
}

/** Thông điệp hiển thị cho người dùng, nội suy bằng `details` (vd. {remaining_attempts}). */
export function errorMessage(
  e: unknown,
  t: (key: string, params?: TParams) => string = translate,
): string {
  const err = toApiError(e);
  const params: TParams = {};
  for (const [k, v] of Object.entries(err.details)) {
    if (typeof v === 'string' || typeof v === 'number') params[k] = v;
  }
  return t(errorKey(err.code), params);
}

// fetch cho trình duyệt (plan G05.03; TS 2.4, 8; LUU_TRU_DU_LIEU 6.4):
// gắn access token, bóc phong bì ApiResponse, 401 GNE-SEC-4010 → làm mới token MỘT lần rồi thử lại.
import { env } from '@/shared/config/env';
import { currentLocale } from '@/shared/lib/locale';
import { getAccessToken, refreshAccessToken, clearSession } from '@/shared/auth/session';
import { ApiError, toApiError } from './errors';
import { IDEMPOTENCY_HEADER } from './idempotency';
import { isPagePagination, type ApiErrorBody, type ApiResponse, type Page } from './types';

export type Query = Record<
  string,
  string | number | boolean | null | undefined | (string | number)[]
>;

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  query?: Query;
  body?: unknown;
  headers?: Record<string, string>;
  /** Bắt buộc cho checkout, thanh toán, rút tiền, hoàn tiền (TS 8). */
  idempotencyKey?: string;
  /** Mặc định true: gắn Bearer token và tự làm mới khi 401. */
  auth?: boolean;
  signal?: AbortSignal;
  /** ms, mặc định 15 s. */
  timeout?: number;
  /** Gốc API; mặc định PUBLIC_API_URL. */
  baseUrl?: string;
}

/** Ghép query string, bỏ giá trị rỗng; mảng → lặp tham số (?id=1&id=2). */
export function buildUrl(base: string, path: string, query?: Query): string {
  const url = new URL(base.replace(/\/$/, '') + (path.startsWith('/') ? path : `/${path}`));
  for (const [k, v] of Object.entries(query ?? {})) {
    if (v === undefined || v === null || v === '') continue;
    if (Array.isArray(v)) v.forEach((x) => url.searchParams.append(k, String(x)));
    else url.searchParams.set(k, String(v));
  }
  return url.toString();
}

async function parseBody(res: Response): Promise<unknown> {
  if (res.status === 204) return null;
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return { code: 'CLIENT-UNKNOWN', status: res.status, message: text.slice(0, 200) };
  }
}

async function send(path: string, o: RequestOptions, token: string | null): Promise<Response> {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), o.timeout ?? 15_000);
  const onAbort = () => ctrl.abort();
  o.signal?.addEventListener('abort', onAbort);
  const headers: Record<string, string> = {
    Accept: 'application/json',
    'Accept-Language': currentLocale(),
    ...o.headers,
  };
  if (o.body !== undefined && !(o.body instanceof FormData))
    headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;
  if (o.idempotencyKey) headers[IDEMPOTENCY_HEADER] = o.idempotencyKey;
  try {
    return await fetch(buildUrl(o.baseUrl ?? env.PUBLIC_API_URL, path, o.query), {
      method: o.method ?? 'GET',
      headers,
      body:
        o.body === undefined
          ? undefined
          : o.body instanceof FormData
            ? o.body
            : JSON.stringify(o.body),
      credentials: 'include',
      signal: ctrl.signal,
    });
  } finally {
    clearTimeout(timer);
    o.signal?.removeEventListener('abort', onAbort);
  }
}

/** Gọi API và trả nguyên phong bì (khi cần `pagination`, `summary`). Lỗi → ném ApiError. */
export async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<ApiResponse<T>> {
  const auth = options.auth ?? true;
  try {
    let res = await send(path, options, auth ? getAccessToken() : null);
    if (res.status === 401 && auth) {
      const body = (await parseBody(res)) as ApiErrorBody | null;
      // Chỉ làm mới khi token hết hạn; lỗi đăng nhập khác (sai mật khẩu…) trả nguyên
      if (!body || body.code === 'GNE-SEC-4010') {
        const ok = await refreshAccessToken();
        if (!ok) {
          clearSession();
          throw new ApiError(
            body ?? { code: 'GNE-SEC-4010', status: 401, message: 'Unauthorized' },
          );
        }
        res = await send(path, options, getAccessToken());
      } else {
        throw new ApiError(body);
      }
    }
    const body = await parseBody(res);
    if (!res.ok) {
      throw new ApiError(
        (body as ApiErrorBody | null) ?? {
          code: 'CLIENT-UNKNOWN',
          status: res.status,
          message: res.statusText,
        },
      );
    }
    return (body ?? {
      code: 'SUCCESS',
      status: res.status,
      message: 'OK',
      data: null,
    }) as ApiResponse<T>;
  } catch (e) {
    throw toApiError(e);
  }
}

/** Gọi API và chỉ lấy `data`. */
export async function http<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return (await request<T>(path, options)).data;
}

/**
 * GET danh sách phân trang số (API_SPEC 0.4): bóc phong bì thành Page<T> kèm `summary` (đếm tab, tổng…).
 * Dùng cho các bảng quản trị (Admin dùng page/size).
 */
export async function getPage<D, T>(
  path: string,
  map: (d: D) => T,
  o?: Omit<RequestOptions, 'method' | 'body'>,
): Promise<Page<T> & { summary: Record<string, unknown> }> {
  const res = await request<D[]>(path, { ...o, method: 'GET' });
  const p = isPagePagination(res.pagination) ? res.pagination : null;
  const items = (res.data ?? []).map(map);
  return {
    items,
    page: p?.page ?? 1,
    size: p?.size ?? items.length,
    total_items: p?.total_items ?? items.length,
    total_pages: p?.total_pages ?? 1,
    summary: res.summary ?? {},
  };
}

export const api = {
  get: <T>(path: string, o?: Omit<RequestOptions, 'method' | 'body'>) =>
    http<T>(path, { ...o, method: 'GET' }),
  post: <T>(path: string, body?: unknown, o?: Omit<RequestOptions, 'method' | 'body'>) =>
    http<T>(path, { ...o, method: 'POST', body }),
  put: <T>(path: string, body?: unknown, o?: Omit<RequestOptions, 'method' | 'body'>) =>
    http<T>(path, { ...o, method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown, o?: Omit<RequestOptions, 'method' | 'body'>) =>
    http<T>(path, { ...o, method: 'PATCH', body }),
  delete: <T>(path: string, o?: Omit<RequestOptions, 'method' | 'body'>) =>
    http<T>(path, { ...o, method: 'DELETE' }),
};

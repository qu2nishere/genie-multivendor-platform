// Lớp mock (plan G05.06; quyết định D3, D5): mọi *.api.ts kiểm USE_MOCK, trả dữ liệu từ *.mock.ts
// qua các hàm dưới đây để có cùng hình dạng với API thật. Một bộ dữ liệu mặc định, không công tắc kịch bản.
import { env } from '@/shared/config/env';
import { ApiError } from './errors';
import type { ApiErrorBody, ApiResponse, CursorPage, Page, Pagination } from './types';

export const USE_MOCK: boolean = env.PUBLIC_USE_MOCK;

const isTest = (): boolean => {
  try {
    return import.meta.env?.MODE === 'test';
  } catch {
    return false;
  }
};

/** Giả độ trễ mạng 150–450 ms (0 khi chạy test / SSR để trang không chậm). */
export function mockDelay(ms?: number): Promise<void> {
  if (isTest() || typeof window === 'undefined') return Promise.resolve();
  const wait = ms ?? 150 + Math.floor(Math.random() * 300);
  return new Promise((r) => setTimeout(r, wait));
}

const meta = () => ({
  request_id: `mock_${Math.random().toString(36).slice(2, 12)}`,
  timestamp: new Date().toISOString(),
  server_version: 'mock',
});

/** Bọc dữ liệu thành phong bì thành công như server. */
export function mockOk<T>(
  data: T,
  extra: { status?: number; pagination?: Pagination; summary?: Record<string, unknown> } = {},
): ApiResponse<T> {
  return {
    code: 'SUCCESS',
    status: extra.status ?? 200,
    message: 'OK',
    data,
    ...(extra.pagination ? { pagination: extra.pagination } : {}),
    ...(extra.summary ? { summary: extra.summary } : {}),
    meta: meta(),
  };
}

/** Ném lỗi đúng định dạng RFC 7807 mở rộng; status suy từ 3 số đầu của mã (GNE-INV-4090 → 409). */
export function mockFail(
  code: string,
  opts: Partial<Pick<ApiErrorBody, 'status' | 'message' | 'errors' | 'details'>> = {},
): never {
  const m = code.match(/-(\d{3})\d$/);
  const status = opts.status ?? (m ? Number(m[1]) : 400);
  throw new ApiError({
    code,
    status,
    message: opts.message ?? code,
    errors: opts.errors,
    details: opts.details,
    meta: meta(),
  });
}

/** Con trỏ giả: mã hóa vị trí bắt đầu (base64) để client không suy diễn. */
const encodeCursor = (offset: number) => btoa(`o:${offset}`);
const decodeCursor = (cursor: string | null | undefined): number => {
  if (!cursor) return 0;
  try {
    const raw = atob(cursor);
    const n = Number(raw.replace(/^o:/, ''));
    return Number.isFinite(n) && n >= 0 ? n : 0;
  } catch {
    return 0;
  }
};

/** Phân trang con trỏ (limit mặc định 20, tối đa 100 — API_SPEC 0.4). */
export function paginateCursor<T>(
  items: readonly T[],
  opts: { limit?: number; cursor?: string | null } = {},
): CursorPage<T> {
  const limit = Math.min(Math.max(opts.limit ?? 20, 1), 100);
  const start = decodeCursor(opts.cursor);
  const slice = items.slice(start, start + limit);
  const next = start + slice.length;
  const has_more = next < items.length;
  return { items: slice, limit, has_more, next_cursor: has_more ? encodeCursor(next) : null };
}

/** Phân trang số trang (size mặc định 20, tối đa 60 — API_SPEC 0.4). Trang vượt quá → mảng rỗng. */
export function paginatePage<T>(
  items: readonly T[],
  opts: { page?: number; size?: number } = {},
): Page<T> {
  const size = Math.min(Math.max(opts.size ?? 20, 1), 60);
  const total_items = items.length;
  const total_pages = Math.max(1, Math.ceil(total_items / size));
  const page = Math.max(1, Math.floor(opts.page ?? 1));
  const start = (page - 1) * size;
  return { items: items.slice(start, start + size), page, size, total_items, total_pages };
}

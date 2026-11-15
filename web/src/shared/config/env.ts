import { z } from 'zod';

// Biến môi trường đọc qua zod (TECH_STACK mục 2.4). Thiếu biến → dùng giá trị dev mặc định.
const bool = z
  .union([z.boolean(), z.string()])
  .optional()
  .transform((v) => v === true || v === 'true' || v === '1');

const schema = z.object({
  PUBLIC_WEB_URL: z.string().url().default('http://localhost:4321'),
  PUBLIC_API_URL: z.string().url().default('http://localhost:8080/api/v1'),
  PUBLIC_S3_URL: z.string().url().optional(),
  INTERNAL_API_URL: z.string().url().optional(),
  PUBLIC_USE_MOCK: bool,
  /** Feature đã nối backend thật dù đang bật mock, cách nhau dấu phẩy (vd. `auth,account`). */
  PUBLIC_REAL_API: z.string().default(''),
});

export type Env = z.infer<typeof schema>;

const empty = (v: string | undefined) => (v === '' ? undefined : v);

export const env: Env = schema.parse({
  PUBLIC_WEB_URL: empty(import.meta.env.PUBLIC_WEB_URL),
  PUBLIC_API_URL: empty(import.meta.env.PUBLIC_API_URL),
  PUBLIC_S3_URL: empty(import.meta.env.PUBLIC_S3_URL),
  INTERNAL_API_URL: empty(import.meta.env.INTERNAL_API_URL),
  // Chưa có backend (plan D3): mặc định bật mock khi biến không được đặt
  PUBLIC_USE_MOCK: import.meta.env.PUBLIC_USE_MOCK ?? 'true',
  PUBLIC_REAL_API: empty(import.meta.env.PUBLIC_REAL_API),
});

const realApi = new Set(
  env.PUBLIC_REAL_API.split(',')
    .map((s) => s.trim())
    .filter(Boolean),
);

/**
 * Nối backend dần từng phân hệ: feature dùng mock khi PUBLIC_USE_MOCK bật VÀ không có trong
 * PUBLIC_REAL_API. Ví dụ `PUBLIC_USE_MOCK=true` + `PUBLIC_REAL_API=auth` → đăng ký / đăng nhập gọi API
 * thật, trang chủ, sản phẩm… vẫn mock (backend chưa có các API đó).
 */
export const isMocked = (feature: string): boolean => env.PUBLIC_USE_MOCK && !realApi.has(feature);

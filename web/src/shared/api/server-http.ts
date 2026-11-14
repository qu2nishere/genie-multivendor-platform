// Gọi API khi SSR (plan G05.04; TS 5.1): frontmatter .astro → INTERNAL_API_URL (http://backend:8080),
// không token (chỉ dữ liệu công khai /public/**), ngôn ngữ theo request hiện tại.
import { env } from '@/shared/config/env';
import { request, type RequestOptions } from './http';
import type { ApiResponse } from './types';

const serverBase = (): string => env.INTERNAL_API_URL ?? env.PUBLIC_API_URL;

export function serverRequest<T>(
  path: string,
  options: Omit<RequestOptions, 'auth' | 'baseUrl'> = {},
): Promise<ApiResponse<T>> {
  return request<T>(path, { timeout: 8_000, ...options, auth: false, baseUrl: serverBase() });
}

export async function serverHttp<T>(
  path: string,
  options: Omit<RequestOptions, 'auth' | 'baseUrl'> = {},
): Promise<T> {
  return (await serverRequest<T>(path, options)).data;
}

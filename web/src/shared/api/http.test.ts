import { afterEach, describe, expect, it, vi } from 'vitest';

// Mô phỏng phiên: token cũ → làm mới 1 lần → token mới
vi.mock('@/shared/auth/session', () => {
  let token: string | null = 'old';
  return {
    getAccessToken: () => token,
    refreshAccessToken: vi.fn(async () => {
      token = 'new';
      return true;
    }),
    clearSession: vi.fn(),
  };
});

import { refreshAccessToken } from '@/shared/auth/session';
import { ApiError } from './errors';
import { buildUrl, http, request } from './http';

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('http', () => {
  it('buildUrl bỏ tham số rỗng, lặp mảng', () => {
    expect(
      buildUrl('https://api.genie.vn/api/v1/', '/products', {
        q: 'tai nghe',
        page: 2,
        x: '',
        ids: [1, 2],
      }),
    ).toBe('https://api.genie.vn/api/v1/products?q=tai+nghe&page=2&ids=1&ids=2');
  });

  it('401 GNE-SEC-4010 → làm mới token một lần rồi gửi lại', async () => {
    const calls: string[] = [];
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_url: string, init: RequestInit) => {
        const auth = (init.headers as Record<string, string>).Authorization;
        calls.push(auth);
        return auth === 'Bearer old'
          ? json(401, { code: 'GNE-SEC-4010', status: 401, message: 'expired' })
          : json(200, { code: 'SUCCESS', status: 200, message: 'OK', data: { id: 1 }, meta: {} });
      }),
    );
    await expect(http('/me')).resolves.toEqual({ id: 1 });
    expect(calls).toEqual(['Bearer old', 'Bearer new']);
    expect(refreshAccessToken).toHaveBeenCalledTimes(1);
  });

  it('lỗi 4xx → ApiError mang mã GNE và details', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        json(409, {
          code: 'GNE-INV-4090',
          status: 409,
          message: 'Hết hàng',
          details: { stock: 0 },
        }),
      ),
    );
    const err = (await request('/checkout', { method: 'POST', idempotencyKey: 'k' }).catch(
      (e: unknown) => e,
    )) as ApiError;
    expect(err).toBeInstanceOf(ApiError);
    expect(err.code).toBe('GNE-INV-4090');
    expect(err.details).toEqual({ stock: 0 });
  });

  it('gắn Idempotency-Key và Accept-Language', async () => {
    const f = vi.fn(async () =>
      json(201, { code: 'SUCCESS', status: 201, message: 'OK', data: null, meta: {} }),
    );
    vi.stubGlobal('fetch', f);
    await http('/checkout-sessions', { method: 'POST', body: {}, idempotencyKey: 'abc' });
    const headers = (f.mock.calls[0] as unknown as [string, RequestInit])[1].headers as Record<
      string,
      string
    >;
    expect(headers['Idempotency-Key']).toBe('abc');
    expect(headers['Accept-Language']).toBe('vi');
    expect(headers['Content-Type']).toBe('application/json');
  });

  it('lỗi mạng → ApiError CLIENT-NETWORK', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))),
    );
    const err = (await http('/x').catch((e: unknown) => e)) as ApiError;
    expect(err.code).toBe('CLIENT-NETWORK');
  });
});

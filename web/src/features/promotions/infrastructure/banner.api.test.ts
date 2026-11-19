// Gom lô sự kiện banner (plan 2026-11-16 banner B06.03; API_SPEC 5.15).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const requestMock = vi.fn().mockResolvedValue({ data: null });

vi.mock('@/shared/api/http', () => ({ request: (...a: unknown[]) => requestMock(...a) }));
vi.mock('@/shared/config/env', () => ({
  env: { PUBLIC_API_URL: 'http://api.test/api/v1', INTERNAL_API_URL: undefined },
  isMocked: () => false,
}));

// vitest chạy môi trường node (repo không có jsdom): giả lập tối thiểu window / document cho trình nghe rời trang
const fakeWindow = new EventTarget();
vi.stubGlobal('window', fakeWindow);
vi.stubGlobal(
  'document',
  Object.assign(new EventTarget(), { visibilityState: 'visible', documentElement: { lang: 'vi' } }),
);

const { trackBanner, resetBannerTrackingForTest } = await import('./banner.api');

describe('trackBanner', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    requestMock.mockClear();
    resetBannerTrackingForTest();
  });
  afterEach(() => vi.useRealTimers());

  it('gom sự kiện 5 giây, mỗi banner chỉ đếm 1 lượt hiển thị', async () => {
    await trackBanner('101', 'view');
    await trackBanner('101', 'view');
    await trackBanner('101', 'click');
    await trackBanner('abc', 'view'); // id không phải số → bỏ qua
    expect(requestMock).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(5_000);
    expect(requestMock).toHaveBeenCalledTimes(1);
    expect(requestMock).toHaveBeenCalledWith('/public/banner-events', {
      method: 'POST',
      auth: false,
      body: {
        events: [
          { banner_id: 101, type: 'IMPRESSION' },
          { banner_id: 101, type: 'CLICK' },
        ],
      },
    });
  });

  it('rời trang thì gửi phần còn lại bằng sendBeacon (text/plain)', async () => {
    const beacon = vi.fn().mockReturnValue(true);
    Object.defineProperty(navigator, 'sendBeacon', { value: beacon, configurable: true });
    await trackBanner('7', 'click');
    fakeWindow.dispatchEvent(new Event('pagehide'));
    expect(beacon).toHaveBeenCalledTimes(1);
    const [url, blob] = beacon.mock.calls[0] as [string, Blob];
    expect(url).toBe('http://api.test/api/v1/public/banner-events');
    expect(blob.type).toBe('text/plain');
    expect(JSON.parse(await blob.text())).toEqual({ events: [{ banner_id: 7, type: 'CLICK' }] });
    await vi.advanceTimersByTimeAsync(5_000);
    expect(requestMock).not.toHaveBeenCalled();
  });
});

import { useMemo } from 'react';
import { useT } from '@/shared/hooks/useT';

/**
 * Ô vuông của mã QR mô phỏng (prototype FakeQr ở storefront + admin) — thuần, có test.
 * Chỉ để hiển thị ở chế độ mock; khi nối backend sẽ thay bằng ảnh QR thật do server cấp.
 */
export function qrCells(seed: string | number, n = 29): [number, number][] {
  const finder = (x: number, y: number) =>
    (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7);
  const ring = (x: number, y: number) => {
    const u = x >= n - 7 ? x - n + 7 : x % (n - 7);
    const v = y >= n - 7 ? y - n + 7 : y % (n - 7);
    return u === 0 || u === 6 || v === 0 || v === 6 || (u >= 2 && u <= 4 && v >= 2 && v <= 4);
  };
  let h = 7;
  for (const c of String(seed)) h = (h * 31 + c.charCodeAt(0)) % 9973;
  const out: [number, number][] = [];
  for (let y = 0; y < n; y++)
    for (let x = 0; x < n; x++) {
      const on = finder(x, y) ? ring(x, y) : (x * 17 + y * 29 + h + x * y * 3) % 7 < 3;
      if (on) out.push([x, y]);
    }
  return out;
}

export function QrPlaceholder({ seed, size = 188 }: { seed: string | number; size?: number }) {
  const { t } = useT();
  const cells = useMemo(() => qrCells(seed), [seed]);
  // Một path thay cho ~400 thẻ rect
  const d = cells.map(([x, y]) => `M${x} ${y}h1v1h-1z`).join('');
  return (
    <svg
      role="img"
      aria-label={t('ui.qr.mock')}
      viewBox="0 0 29 29"
      width={size}
      height={size}
      shapeRendering="crispEdges"
    >
      <path d={d} fill="#14171F" />
    </svg>
  );
}

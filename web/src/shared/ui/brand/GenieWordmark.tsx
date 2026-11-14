import { GENIE_WORDMARK } from './genieLettering';

/**
 * Chữ "Genie" kiểu chữ logo (prototype: .tr-logo span / .tr-foot__brand dùng font "Genie Script").
 * Vẽ bằng path nên không cần tệp font; `fontPx` = cỡ chữ tương đương trong prototype.
 */
export function GenieWordmark({ fontPx = 34, className }: { fontPx?: number; className?: string }) {
  const h = fontPx;
  const w = (GENIE_WORDMARK.width / GENIE_WORDMARK.height) * fontPx;
  return (
    <svg
      className={className}
      width={w}
      height={h}
      viewBox={`0 0 ${GENIE_WORDMARK.width} ${GENIE_WORDMARK.height}`}
      aria-hidden="true"
      focusable="false"
      style={{ overflow: 'visible' }}
    >
      <path fill="currentColor" d={GENIE_WORDMARK.d} />
    </svg>
  );
}

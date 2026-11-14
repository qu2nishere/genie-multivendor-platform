import { GENIE_COIN_TEXT } from './genieLettering';

export interface GenieCoinProps {
  size?: number;
  className?: string;
  title?: string;
}

/**
 * Đồng xu vàng cố định màu (genie-coin.svg) — biểu tượng Điểm Genie, không đổi theo theme.
 * Viết lại từ assets/brand/genie-coin.js (plan G03.07); bỏ lớp ảnh nền PNG để nhẹ.
 */
export function GenieCoin({ size = 20, className, title }: GenieCoinProps) {
  const labelled = Boolean(title);
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 1080 1080"
      role={labelled ? 'img' : undefined}
      aria-hidden={labelled ? undefined : true}
      aria-label={title}
      focusable="false"
    >
      <circle cx="540" cy="540" r="540" fill="#F7D472" />
      <circle cx="540" cy="540" r="486" fill="#EDC148" />
      <circle cx="540" cy="540" r="437.4" fill="#DBA90B" />
      <path
        fill="#DBA90B"
        d="M113.35 436.17H966.65A437.4 471.17 0 0 1 966.65 643.83H113.35A437.4 471.17 0 0 1 113.35 436.17Z"
      />
      <path fill="#FFFFFF" d={GENIE_COIN_TEXT} />
    </svg>
  );
}

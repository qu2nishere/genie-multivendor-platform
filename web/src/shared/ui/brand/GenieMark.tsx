import coinBackdrop from '@/assets/brand/coin-backdrop.png';
import { GENIE_COIN_TEXT } from './genieLettering';

export interface GenieMarkProps {
  /** Cạnh vuông tính bằng px. Từ 48 px trở lên vẽ thêm lớp nền dập nổi. */
  size?: number;
  className?: string;
  /** Có nhãn thì là ảnh có nghĩa; mặc định chỉ để trang trí. */
  title?: string;
}

/**
 * Đồng xu Genie (logo/Webicon.svg) vẽ nội tuyến để mỗi theme tô lại màu:
 * ba vòng + băng ngang lấy --mark-1..3, chữ lấy --mark-ink (đặt trong styles/themes.css).
 * Chuyển từ brand.jsx → GenieMark (plan G03.06).
 */
export function GenieMark({ size = 32, className, title }: GenieMarkProps) {
  const labelled = Boolean(title);
  return (
    <svg
      className={['tr-mark', className].filter(Boolean).join(' ')}
      width={size}
      height={size}
      viewBox="0 0 1080 1080"
      role={labelled ? 'img' : undefined}
      aria-hidden={labelled ? undefined : true}
      aria-label={title}
      focusable="false"
    >
      <circle cx="540" cy="540" r="540" fill="var(--mark-1, #F7D472)" />
      <circle cx="540" cy="540" r="486" fill="var(--mark-2, #EDC148)" />
      <circle cx="540" cy="540" r="437.4" fill="var(--mark-3, #DBA90B)" />
      {size >= 48 && (
        <image
          href={coinBackdrop.src}
          x="99.12"
          y="109.74"
          width="873.36"
          height="860.4"
          preserveAspectRatio="none"
          opacity=".44"
        />
      )}
      <path
        fill="var(--mark-3, #DBA90B)"
        d="M113.35 436.17H966.65A437.4 471.17 0 0 1 966.65 643.83H113.35A437.4 471.17 0 0 1 113.35 436.17Z"
      />
      <path fill="var(--mark-ink, #FFFFFF)" d={GENIE_COIN_TEXT} />
    </svg>
  );
}

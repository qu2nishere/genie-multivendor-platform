import { GenieMark } from './GenieMark';
import { GENIE_WORDMARK } from './genieLettering';

export interface GenieLogoProps {
  /** Cỡ đồng xu (px). */
  size?: number;
  /** Dòng phụ dưới chữ, ví dụ "Seller Center", "Admin Console". */
  sub?: string;
  /** "light" cho nền tối (thanh bên Seller / Admin, cột thương hiệu trang đăng nhập). */
  tone?: 'light';
  className?: string;
}

/** Cỡ chữ của wordmark trong prototype: 30px (.gn-logo__word). */
const WORD_FONT_PX = 30;

/** Đồng xu + chữ "Genie" kiểu chữ của logo (brand.jsx → GenieLogo, plan G03.06). */
export function GenieLogo({ size = 32, sub, tone, className }: GenieLogoProps) {
  const h = WORD_FONT_PX * 0.9;
  const w = (GENIE_WORDMARK.width / GENIE_WORDMARK.height) * WORD_FONT_PX;
  return (
    <span
      className={[
        'gn-logo inline-flex items-center gap-[9px]',
        tone === 'light' ? 'text-white' : 'text-[color:var(--logo-body,var(--brand-900))]',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      data-tone={tone}
    >
      <GenieMark size={size} />
      <span className="gn-logo__txt flex flex-col leading-none">
        <svg
          className="gn-logo__word pr-[2px]"
          width={w}
          height={h}
          viewBox={`0 ${GENIE_WORDMARK.height * 0.05} ${GENIE_WORDMARK.width} ${GENIE_WORDMARK.height * 0.9}`}
          role="img"
          aria-label="Genie"
          focusable="false"
          style={{ overflow: 'visible' }}
        >
          <path fill="currentColor" d={GENIE_WORDMARK.d} />
        </svg>
        {sub && (
          <small className="mt-[9px] font-sans text-[10px] font-bold uppercase leading-none tracking-[.12em] opacity-[.72]">
            {sub}
          </small>
        )}
      </span>
    </span>
  );
}

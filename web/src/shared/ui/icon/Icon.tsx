import type { CSSProperties, SVGProps } from 'react';
import { ICONS, type IconName } from './icons';

export interface IconProps extends Omit<
  SVGProps<SVGSVGElement>,
  'name' | 'dangerouslySetInnerHTML'
> {
  name: IconName;
  size?: number;
  strokeWidth?: number;
  /** Có title → icon mang nghĩa (role="img"); không có → aria-hidden. */
  title?: string;
  style?: CSSProperties;
}

const escapeText = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

/** Icon SVG nội tuyến từ bảng ICONS của prototype (plan G06.01). */
export function Icon({
  name,
  size = 20,
  strokeWidth = 2,
  title,
  className,
  style,
  ...rest
}: IconProps) {
  const inner = ICONS[name];
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={{ display: 'block', flex: 'none', ...style }}
      aria-hidden={title ? undefined : true}
      role={title ? 'img' : undefined}
      focusable="false"
      {...rest}
      // Nội dung là hằng tĩnh trong icons.ts; title được thoát ký tự HTML
      dangerouslySetInnerHTML={{
        __html: (title ? `<title>${escapeText(title)}</title>` : '') + inner,
      }}
    />
  );
}

export type { IconName };

/** Chuỗi bất kỳ (icon danh mục / thông báo từ dữ liệu) → IconName hợp lệ, không có thì dùng `fallback`. */
export const toIconName = (s: string, fallback: IconName = 'package'): IconName =>
  Object.prototype.hasOwnProperty.call(ICONS, s) ? (s as IconName) : fallback;

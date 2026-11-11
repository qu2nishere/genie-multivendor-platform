import { cn } from '@/shared/lib/cn';

export interface AvatarProps {
  src?: string | null;
  name?: string;
  size?: number;
  /** true / false hiện chấm trực tuyến / ngoại tuyến; bỏ trống thì không hiện. */
  online?: boolean;
  square?: boolean;
  className?: string;
}

/** Bảng màu nền chữ cái đầu (prototype Avatar). */
const PALETTE = ['#1D4ED8', '#0369A1', '#6C2BD9', '#0E8A52', '#C8102E', '#B45309'];

export function initialsOf(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map((w) => w[0])
    .slice(-2)
    .join('')
    .toUpperCase();
}

export const avatarColor = (name: string) => PALETTE[(name.charCodeAt(0) || 0) % PALETTE.length];

/** Ảnh đại diện tròn hoặc chữ cái đầu, tùy chọn chấm trực tuyến (plan G06.11). */
export function Avatar({ src, name = '?', size = 36, online, square, className }: AvatarProps) {
  return (
    <span className={cn('relative inline-flex flex-none', className)}>
      <span
        className={cn(
          'grid place-items-center overflow-hidden text-white font-bold',
          square ? 'rounded-md' : 'rounded-full',
        )}
        style={{
          width: size,
          height: size,
          background: src ? 'var(--gray-200)' : avatarColor(name),
          fontSize: Math.round(size * 0.4),
        }}
        aria-hidden={src ? undefined : true}
      >
        {src ? (
          <img
            src={src}
            alt={name}
            width={size}
            height={size}
            className="w-full h-full object-cover"
          />
        ) : (
          initialsOf(name)
        )}
      </span>
      {online != null && (
        <span
          aria-hidden="true"
          className={cn(
            'absolute right-0 bottom-0 rounded-full shadow-[0_0_0_2px_#fff]',
            online ? 'bg-mint-500' : 'bg-gray-400',
          )}
          style={{ width: size * 0.28, height: size * 0.28 }}
        />
      )}
    </span>
  );
}

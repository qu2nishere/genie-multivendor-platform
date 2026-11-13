// Mảnh giao diện dùng chung của các form xác thực (prototype auth/index.html: tiêu đề, nút quay lại,
// dòng "HOẶC", hộp thông báo, biểu tượng tròn của màn khóa / thành công).
import type { ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import { Icon, type IconName } from '@/shared/ui';

export function AuthHeading({
  title,
  lead,
  center,
}: {
  title: ReactNode;
  lead?: ReactNode;
  center?: boolean;
}) {
  return (
    <div className={cn('mb-6', center && 'text-center')}>
      <h1 className="m-0 mb-1.5 text-[28px] font-bold leading-tight text-strong max-md:text-2xl">
        {title}
      </h1>
      {lead && <p className="m-0 text-base text-muted">{lead}</p>}
    </div>
  );
}

export function BackButton({ onClick, children }: { onClick: () => void; children: ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="mb-5 inline-flex min-h-[44px] cursor-pointer items-center gap-1.5 border-0 bg-transparent p-0 text-sm text-muted hover:text-primary"
    >
      <Icon name="chevron-left" size={16} />
      {children}
    </button>
  );
}

export function OrDivider({ children }: { children: ReactNode }) {
  return (
    <div className="my-6 flex items-center gap-3" role="separator">
      <span className="h-px flex-1 bg-line" />
      <span className="text-xs text-subtle">{children}</span>
      <span className="h-px flex-1 bg-line" />
    </div>
  );
}

export function Notice({
  tone,
  title,
  children,
}: {
  tone: 'ok' | 'error';
  title?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={cn(
        'mb-[18px] flex items-start gap-[9px] rounded-md px-[13px] py-[11px] text-sm',
        tone === 'ok' ? 'bg-mint-50 text-mint-600' : 'bg-red-50 text-red-600',
      )}
    >
      <Icon
        name={tone === 'ok' ? 'circle-check' : 'circle-alert'}
        size={17}
        className="mt-px flex-none"
      />
      <span>
        {title && <b>{title}. </b>}
        {children}
      </span>
    </div>
  );
}

export function StateIcon({ icon, tone }: { icon: IconName; tone: 'ok' | 'error' }) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        'mx-auto mb-[22px] grid h-20 w-20 place-items-center rounded-full',
        tone === 'ok' ? 'bg-mint-50 text-mint-600' : 'bg-red-50 text-red-600',
      )}
    >
      <Icon name={icon} size={40} />
    </div>
  );
}

/** Gợi ý dữ liệu thử khi chạy mock (prototype ghi "demo: 123456"). */
export function DemoHint({ children }: { children: ReactNode }) {
  return <span className="text-xs text-subtle">{children}</span>;
}

import type { HTMLAttributes, ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';

// VoucherTag + Ticket (plan G06.25). Ba tầng voucher của Genie: sàn / shop / freeship.
export type VoucherKind = 'platform' | 'shop' | 'freeship';

export interface VoucherTagProps extends HTMLAttributes<HTMLSpanElement> {
  kind?: VoucherKind;
  /** Mức giảm đã định dạng ("-30k", "15%"). */
  amount: ReactNode;
  label: ReactNode;
}

const TAG_BG: Record<VoucherKind, string> = {
  platform: 'bg-primary',
  shop: 'bg-flash-500',
  freeship: 'bg-mint-500',
};

/** Nhãn voucher kiểu vé đục lỗ (.lz-voucher). */
export function VoucherTag({
  kind = 'platform',
  amount,
  label,
  className,
  ...rest
}: VoucherTagProps) {
  const { t } = useT();
  return (
    <span
      className={cn(
        'lz-voucher relative inline-flex items-center gap-2.5 py-[9px] pr-3.5 pl-4 rounded-md overflow-hidden text-white font-semibold text-sm leading-[1.2]',
        'before:content-[""] before:absolute before:left-[54px] before:-top-1.5 before:w-3 before:h-3 before:rounded-full before:bg-surface-page',
        'after:content-[""] after:absolute after:left-[54px] after:-bottom-1.5 after:w-3 after:h-3 after:rounded-full after:bg-surface-page',
        TAG_BG[kind],
        className,
      )}
      {...rest}
    >
      <span className="font-black text-lg">{amount}</span>
      <span className="flex flex-col leading-tight">
        <span className="text-2xs uppercase tracking-caps opacity-[.85]">
          {t(`ui.voucher.kind.${kind}`)}
        </span>
        <span>{label}</span>
      </span>
    </span>
  );
}

export interface TicketProps {
  kind?: VoucherKind;
  amount: ReactNode;
  label: ReactNode;
  /** Dòng loại; mặc định theo kind (Voucher Genie / Voucher của shop / Freeship). */
  kindLabel?: ReactNode;
  /** Ô hành động bên phải (nút Lưu / Dùng / đã lưu). */
  action?: ReactNode;
  /** Dòng phụ (HSD, điều kiện). */
  note?: ReactNode;
  disabled?: boolean;
  className?: string;
}

const AMT_BG: Record<VoucherKind, string> = {
  platform: 'bg-brand-500',
  shop: 'bg-ink-800',
  freeship: 'bg-mint-500',
};

/** Phiếu voucher kiểu vé giấy (.tr-ticket: cuống màu + thân, đường cắt nét đứt). Theme đè qua lớp tr-ticket. */
export function Ticket({
  kind = 'platform',
  amount,
  label,
  kindLabel,
  action,
  note,
  disabled,
  className,
}: TicketProps) {
  const { t } = useT();
  return (
    <div
      className={cn(
        'tr-ticket',
        `tr-ticket--${kind}`,
        'flex items-stretch min-h-[56px] bg-surface-card border border-line rounded-lg overflow-hidden',
        disabled && 'opacity-60',
        className,
      )}
    >
      <span
        className={cn(
          'tr-ticket__amt flex-none w-16 grid place-items-center px-1.5 text-white text-center font-extrabold text-base leading-none tracking-[-.02em]',
          AMT_BG[kind],
        )}
      >
        {amount}
      </span>
      <span className="tr-ticket__body flex-1 min-w-0 flex flex-col justify-center gap-0.5 py-1.5 px-2.5 border-l border-dashed border-line-strong">
        <small className="font-semibold text-xs leading-[1.3] text-muted">
          {kindLabel ?? t(`ui.voucher.ticket.${kind}`)}
        </small>
        <b className="font-bold text-[13px] leading-[1.3] text-strong">{label}</b>
        {note && <span className="text-2xs text-subtle">{note}</span>}
      </span>
      {action && <span className="flex-none self-center mr-2.5">{action}</span>}
    </div>
  );
}

/** Nút "Lưu" vàng của phiếu (.tr-ticket__save); đã lưu → dấu tích xanh. */
export function TicketSaveButton({
  saved,
  onSave,
  label,
}: {
  saved: boolean;
  onSave: () => void;
  label: string;
}) {
  const { t } = useT();
  return (
    <button
      type="button"
      onClick={onSave}
      disabled={saved}
      aria-label={
        saved ? t('ui.voucher.saved_aria', { label }) : t('ui.voucher.save_aria', { label })
      }
      title={saved ? t('ui.voucher.saved_title') : undefined}
      className={cn(
        'tr-ticket__save inline-flex items-center justify-center gap-1 rounded-pill border-0 font-bold text-xs cursor-pointer',
        'focus-visible:outline-none focus-visible:shadow-focus',
        saved
          ? 'w-[30px] h-[30px] p-0 bg-green-50 text-green-600 cursor-default'
          : 'h-[30px] min-w-[52px] px-3 bg-accent text-accent-on hover:bg-accent-hover',
      )}
    >
      {saved ? (
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.8"
          aria-hidden="true"
        >
          <path d="M20 6 9 17l-5-5" />
        </svg>
      ) : (
        t('ui.voucher.save')
      )}
    </button>
  );
}

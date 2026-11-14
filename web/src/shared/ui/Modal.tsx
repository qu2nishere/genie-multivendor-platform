import { useEffect, useId, useRef, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { useFocusTrap } from '@/shared/hooks/useFocusTrap';
import { useScrollLock } from '@/shared/hooks/useScrollLock';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Button } from './Button';
import { Icon } from './icon/Icon';

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: ReactNode;
  /** Nhãn ẩn khi không có tiêu đề hiển thị. */
  'aria-label'?: string;
  children?: ReactNode;
  footer?: ReactNode;
  /** Bề rộng tối đa (px); mặc định 460 như prototype. */
  width?: number;
  /** Không đóng khi bấm nền (form đang nhập dở). */
  dismissible?: boolean;
  className?: string;
  bodyClassName?: string;
}

/**
 * Hộp thoại giữa màn hình (prototype Modal) — plan G06.12: bẫy focus, Esc để đóng, khóa cuộn,
 * aria-modal, trả focus về nút mở. Render qua portal vào <body>.
 */
export function Modal(props: ModalProps) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  if (!props.open || !mounted) return null;
  return createPortal(<ModalDialog {...props} />, document.body);
}

function ModalDialog({
  onClose,
  title,
  'aria-label': ariaLabel,
  children,
  footer,
  width = 460,
  dismissible = true,
  className,
  bodyClassName,
}: ModalProps) {
  const { t } = useT();
  const box = useRef<HTMLDivElement>(null);
  const labelId = useId();
  const close = useRef(onClose);
  close.current = onClose;
  useFocusTrap(box, true);
  useScrollLock(true);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        close.current();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, []);

  return (
    <div
      className="fixed inset-0 z-modal grid place-items-center p-2 md:p-6 bg-surface-overlay animate-[lzFade_.18s_ease-out]"
      onMouseDown={(e) => {
        if (dismissible && e.target === e.currentTarget) onClose();
      }}
    >
      <div
        ref={box}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? labelId : undefined}
        aria-label={title ? undefined : ariaLabel}
        tabIndex={-1}
        style={{ width }}
        className={cn(
          'max-w-[calc(100vw-16px)] md:max-w-full max-h-[88vh] overflow-auto flex flex-col bg-surface-card rounded-xl shadow-xl outline-none',
          'animate-[lzPop_.2s_var(--ease-out)]',
          className,
        )}
      >
        {title && (
          <div className="flex items-center gap-3 py-3 pr-3 pl-5 border-b border-line-subtle">
            <h2 id={labelId} className="flex-1 m-0 font-bold text-lg leading-snug text-strong">
              {title}
            </h2>
            <button
              type="button"
              onClick={onClose}
              aria-label={t('ui.close')}
              data-skip-autofocus
              className="w-11 h-11 grid place-items-center rounded-md text-muted hover:bg-gray-100 focus-visible:outline-none focus-visible:shadow-focus"
            >
              <Icon name="x" size={20} />
            </button>
          </div>
        )}
        <div className={cn('px-5 py-[18px] flex-1', bodyClassName)}>{children}</div>
        {footer && (
          <div className="flex justify-end flex-wrap gap-2.5 px-5 py-3.5 border-t border-line-subtle">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}

export interface ConfirmDialogProps {
  open: boolean;
  title: ReactNode;
  message?: ReactNode;
  confirmLabel?: ReactNode;
  cancelLabel?: ReactNode;
  tone?: 'primary' | 'danger';
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  children?: ReactNode;
}

/** Hộp xác nhận (hủy đơn, xóa địa chỉ, đăng xuất thiết bị…). */
export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel,
  cancelLabel,
  tone = 'primary',
  loading,
  onConfirm,
  onCancel,
  children,
}: ConfirmDialogProps) {
  const { t } = useT();
  return (
    <Modal
      open={open}
      onClose={onCancel}
      title={title}
      width={420}
      dismissible={!loading}
      footer={
        <>
          <Button variant="outline" onClick={onCancel} disabled={loading}>
            {cancelLabel ?? t('common.action.cancel')}
          </Button>
          <Button
            variant={tone === 'danger' ? 'danger' : 'primary'}
            onClick={onConfirm}
            loading={loading}
          >
            {confirmLabel ?? t('common.action.confirm')}
          </Button>
        </>
      }
    >
      {message && <p className="text-base text-body">{message}</p>}
      {children}
    </Modal>
  );
}

import { useStore } from '@nanostores/react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon, type IconName } from './icon/Icon';
import { $toasts, dismissToast, type ToastType } from './toast.store';

export { toast, $toasts, type Toast, type ToastType } from './toast.store';

const ICON: Record<ToastType, IconName> = {
  info: 'info',
  success: 'circle-check',
  error: 'circle-alert',
};
const TONE: Record<ToastType, string> = {
  info: 'text-primary',
  success: 'text-mint-600',
  error: 'text-red-500',
};

/**
 * Vùng hiện toast (prototype .tr-toast: góc trên phải, nền trắng, viền mảnh, trượt xuống).
 * Đặt một lần trong layout (ToastHost). role=status / alert để trình đọc màn hình đọc.
 */
export function ToastViewport() {
  const toasts = useStore($toasts);
  const { t } = useT();
  return (
    <div
      aria-live="polite"
      className="tr-toasts fixed z-toast top-2.5 left-3 right-3 md:left-auto md:top-8 md:right-8 flex flex-col items-stretch md:items-end gap-2 pointer-events-none"
    >
      {toasts.map((x) => (
        <div
          key={x.id}
          role={x.type === 'error' ? 'alert' : 'status'}
          className={cn(
            'tr-toast pointer-events-auto flex items-center gap-3 max-w-full md:max-w-[420px]',
            'bg-surface-card text-strong px-3.5 py-3 md:px-6 md:py-3.5 rounded-[8px] border border-line',
            'shadow-[0_4px_12px_rgba(0,0,0,.15)] animate-[lzSlideDown_.25s_var(--ease-out)]',
          )}
        >
          <Icon name={ICON[x.type]} size={22} className={TONE[x.type]} />
          <span className="flex-1 text-base font-medium">{x.message}</span>
          <button
            type="button"
            onClick={() => dismissToast(x.id)}
            aria-label={t('ui.close')}
            className="-mr-2 w-8 h-8 grid place-items-center rounded-sm text-subtle hover:text-body focus-visible:outline-none focus-visible:shadow-focus"
          >
            <Icon name="x" size={16} />
          </button>
        </div>
      ))}
    </div>
  );
}

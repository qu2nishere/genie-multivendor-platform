import { useEffect, useState } from 'react';
import { secondsUntilMidnightVN, useCountdown } from '@/shared/hooks/useCountdown';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatHhMmSs, formatMmSs } from '@/shared/lib/format';

export interface CountdownProps {
  /** Số giây đếm ngược; bỏ trống + until="midnight" để đếm tới 24:00 giờ Việt Nam. */
  seconds?: number;
  until?: 'midnight';
  format?: 'hh:mm:ss' | 'mm:ss';
  /**
   * clock: nhãn "Kết thúc sau" + đồng hồ (Flash Band trang chủ, .tr-clock)
   * amber: mm:ss màu hổ phách (giữ chỗ 15 phút ở checkout)
   * plain: chỉ chữ số mono (UntilMidnight trong Điểm)
   */
  variant?: 'clock' | 'amber' | 'plain';
  onEnd?: () => void;
  /** Nhãn đọc màn hình của đồng hồ clock (mặc định "Kết thúc lúc 24:00 hôm nay"). */
  label?: string;
  /** Chữ nhỏ trước đồng hồ clock (mặc định "Kết thúc sau"). */
  prefix?: string;
  className?: string;
}

/** Đồng hồ đếm ngược dùng chung (G06.17) — gộp Countdown của Home, Checkout và UntilMidnight. */
export function Countdown({
  seconds,
  until,
  format,
  variant = 'plain',
  onEnd,
  label,
  prefix,
  className,
}: CountdownProps) {
  const { t } = useT();
  // Đếm tới nửa đêm: tính lại theo đồng hồ thật mỗi giây để tải lại trang không đặt lại
  const [midnight, setMidnight] = useState<number | null>(() => (until === 'midnight' ? null : 0));
  useEffect(() => {
    if (until !== 'midnight') return undefined;
    const tick = () => setMidnight(secondsUntilMidnightVN());
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [until]);
  const { left } = useCountdown(seconds ?? 0, until !== 'midnight');
  const value = until === 'midnight' ? midnight : left;

  useEffect(() => {
    if (value === 0 && onEnd) onEnd();
  }, [value, onEnd]);

  const fmt = format ?? (until === 'midnight' ? 'hh:mm:ss' : 'mm:ss');
  // SSR chưa biết giờ máy khách: hiện chỗ trống cố định độ dài để không lệch bố cục
  const text =
    value === null
      ? fmt === 'hh:mm:ss'
        ? '--:--:--'
        : '--:--'
      : fmt === 'hh:mm:ss'
        ? formatHhMmSs(value)
        : formatMmSs(value);

  if (variant === 'clock') {
    return (
      <span
        className={cn('tr-clock', className)}
        role="timer"
        aria-label={label ?? t('ui.countdown.ends_midnight')}
      >
        <small>{prefix ?? t('ui.countdown.ends_in')}</small>
        {text}
      </span>
    );
  }
  return (
    <span
      role="timer"
      className={cn('code', variant === 'amber' && 'font-bold text-amber-600', className)}
    >
      {text}
    </span>
  );
}

// Đường tiền "Genie giữ tiền" (prototype Shared.jsx MoneyRail; DESIGN.md signature) — G10.23 (dùng sớm ở
// hero trang chủ). Tương tác: tự chạy mỗi 3,6 s một lượt rồi dừng, bấm từng bước để xem; cố định: hiện
// một bước (biên nhận). Câu mô tả đổi theo bước và được đọc nhẹ cho trình đọc màn hình.
import { useCallback, useEffect, useRef, useState } from 'react';
import { useMediaQuery, MQ } from '@/shared/hooks/useMediaQuery';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { cn } from '@/shared/lib/cn';
import { Icon } from '@/shared/ui';
import { AUTO_STEP_MS, coinLeftPct, fillPct, MONEY_STEPS } from '../domain/escrow';

export interface MoneyRailProps {
  amount: number;
  shop?: string;
  /** Bước bắt đầu (mặc định "Genie giữ tiền"). */
  start?: number;
  /** Điều khiển từ ngoài (cổng thanh toán mô phỏng di chuyển đồng xu). */
  step?: number;
  interactive?: boolean;
  /** Bản nền trắng không kính (biên nhận). */
  flat?: boolean;
}

export default function MoneyRail({
  amount,
  shop,
  start = 1,
  step: at,
  interactive = false,
  flat = false,
}: MoneyRailProps) {
  const { t, locale } = useT();
  const reduced = useMediaQuery(MQ.reducedMotion);
  const [step, setStep] = useState(start);
  const [moving, setMoving] = useState(false);
  const [auto, setAuto] = useState(interactive);
  const last = MONEY_STEPS.length - 1;

  const stepRef = useRef(step);
  stepRef.current = step;
  const go = useCallback((i: number) => {
    setMoving(true);
    setStep(i);
    setTimeout(() => setMoving(false), 700);
  }, []);

  // Bước điều khiển từ ngoài (cổng thanh toán mô phỏng)
  useEffect(() => {
    if (typeof at === 'number' && at !== stepRef.current) go(at);
  }, [at, go]);

  useEffect(() => {
    if (!auto || reduced) return undefined;
    const h = setInterval(() => {
      setStep((s) => {
        const next = Math.min(s + 1, last);
        if (next === last) setAuto(false);
        if (next !== s) {
          setMoving(true);
          setTimeout(() => setMoving(false), 700);
        }
        return next;
      });
    }, AUTO_STEP_MS);
    return () => clearInterval(h);
  }, [auto, reduced, last]);

  const money = formatVND(amount, { locale });
  return (
    <div className={cn('tr-rail', flat ? 'tr-rail--flat' : 'tr-glass')}>
      <div className="tr-rail__head">
        <span>
          {interactive ? t('payment.rail.example') : t('payment.rail.yours')} <b>{money}</b>
          {shop ? ` ${t('payment.rail.at', { shop })}` : ''}
        </span>
        {interactive && <span>{t('payment.rail.hint')}</span>}
      </div>
      <div className="tr-rail__track">
        <div className="tr-rail__line">
          <div className="tr-rail__fill" style={{ width: `${fillPct(step)}%` }} />
        </div>
        <span
          className="tr-rail__drop"
          data-moving={moving ? 1 : 0}
          style={{ left: `${coinLeftPct(step)}%` }}
          aria-hidden="true"
        >
          <svg viewBox="0 0 42 42">
            <circle
              cx="21"
              cy="21"
              r="19.5"
              fill="var(--coin, #FFC02E)"
              stroke="#fff"
              strokeWidth="2"
            />
            <circle
              cx="21"
              cy="21"
              r="14.5"
              fill="none"
              stroke="var(--coin-ring, #8A6100)"
              strokeOpacity=".35"
              strokeWidth="1.5"
            />
          </svg>
          <Icon name="lock" size={16} strokeWidth={2.4} />
        </span>
        {MONEY_STEPS.map((s, i) => (
          <button
            key={s}
            type="button"
            className="tr-rail__stop"
            disabled={!interactive}
            data-done={i <= step ? 1 : 0}
            aria-current={i === step ? 'step' : undefined}
            onClick={() => {
              setAuto(false);
              go(i);
            }}
          >
            <span className="tr-rail__dot">
              <i />
            </span>
            {t(`payment.rail.step.${s}`)}
          </button>
        ))}
      </div>
      <p className="tr-rail__note" aria-live="polite">
        {t(`payment.rail.note.${MONEY_STEPS[step]}`)}
      </p>
    </div>
  );
}

import type { ReactNode } from 'react';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatNumber } from '@/shared/lib/format';
import { Icon, type IconName } from '../icon/Icon';
import { change } from './dash.math';

// Delta, StatTile, Kpi (plan G06.29) — lớp dash-* trong styles/dash.css.

export interface DeltaProps {
  cur: number;
  prev?: number | null;
  /** "so với tuần trước" … */
  against?: string;
  /** Giảm là tốt (chi phí, tỉ lệ hủy). */
  downIsGood?: boolean;
}

/** Thay đổi so với kỳ trước: mũi tên + phần trăm, xanh khi tốt, đỏ khi xấu. */
export function Delta({ cur, prev, against, downIsGood }: DeltaProps) {
  const { t, locale } = useT();
  const p = change(cur, prev);
  if (p === null) {
    return (
      <span>{against ? t('ui.dash.no_data_against', { against }) : t('ui.dash.no_data')}</span>
    );
  }
  const flat = Math.abs(p) < 0.05;
  const up = p > 0;
  const good = downIsGood ? !up : up;
  const n = formatNumber(Math.round(p * 10) / 10, { locale });
  return (
    <span>
      <span className={cn('dash-delta', !flat && (good ? 'dash-delta--good' : 'dash-delta--bad'))}>
        {!flat && <Icon name={up ? 'arrow-up-right' : 'arrow-down-right'} size={13} />}
        {(up ? '+' : '') + n}%
      </span>
      {against ? ` ${against}` : ''}
    </span>
  );
}

export interface StatTileProps {
  label: ReactNode;
  value: ReactNode;
  note?: ReactNode;
  /** Có thì ô là nút dẫn tới màn chi tiết (prototype: chỉ là nút khi dẫn đi đâu đó). */
  onClick?: () => void;
  href?: string;
}

/** Ô số liệu dùng chung mọi bảng điều khiển. Số lớn thu nhỏ ở màn hẹp (responsive.css prototype). */
export function StatTile({ label, value, note, onClick, href }: StatTileProps) {
  const inner = (
    <>
      <span className="dash-tile__label">
        {label}
        {(onClick || href) && <Icon name="chevron-right" size={15} />}
      </span>
      <span className="dash-tile__value max-md:text-base max-md:[overflow-wrap:anywhere]">
        {value}
      </span>
      {note && <span className="dash-tile__note">{note}</span>}
    </>
  );
  if (href) {
    return (
      <a href={href} className="dash-tile no-underline hover:no-underline">
        {inner}
      </a>
    );
  }
  if (onClick) {
    return (
      <button type="button" className="dash-tile" onClick={onClick}>
        {inner}
      </button>
    );
  }
  return <div className="dash-tile">{inner}</div>;
}

export interface KpiProps {
  label: ReactNode;
  value: ReactNode;
  icon?: IconName;
  /** Cặp màu ô icon (prototype Catalog Kpi tone). */
  tone?: { fg: string; bg: string };
}

/** KPI có ô icon màu (prototype admin Catalog Kpi); không icon thì như StatTile. */
export function Kpi({ label, value, icon, tone }: KpiProps) {
  if (!icon) return <StatTile label={label} value={value} />;
  return (
    <div className="bg-surface-card rounded-lg shadow-sm py-4 px-[18px] min-w-0">
      <div className="flex items-center gap-2 mb-2.5">
        <span
          className="w-8 h-8 rounded-md grid place-items-center flex-none"
          style={{
            background: tone?.bg ?? 'var(--brand-50)',
            color: tone?.fg ?? 'var(--brand-600)',
          }}
        >
          <Icon name={icon} size={17} />
        </span>
        <span className="font-medium text-[13px] text-muted">{label}</span>
      </div>
      <div className="font-extrabold text-2xl max-md:text-base text-strong tabular-nums">
        {value}
      </div>
    </div>
  );
}

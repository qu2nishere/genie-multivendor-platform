import type { CSSProperties } from 'react';
import { useT } from '@/shared/hooks/useT';
import { formatNumber, formatPercent } from '@/shared/lib/format';
import { Icon } from '../icon/Icon';
import { shareOf, type Tone } from './dash.math';

// WorkQueue, Pipeline (plan G06.30).

export interface Job {
  id: string;
  count: number;
  label: string;
  hint: string;
  /** Liên kết tới màn xử lý (SPA dùng href của router). */
  href?: string;
  onClick?: () => void;
}

/** Hàng việc cần làm — thứ đầu tiên trên bảng điều khiển; việc đang chờ nền vàng, hết việc thì nhạt. */
export function WorkQueue({
  title,
  jobs,
  doneText,
}: {
  title: string;
  jobs: Job[];
  doneText: string;
}) {
  const { t, locale } = useT();
  const total = jobs.reduce((n, j) => n + j.count, 0);
  return (
    <section className="dash-sheet" aria-label={title}>
      <div className="dash-head">
        <h2 className="dash-title">{title}</h2>
        <span className="dash-sub">
          {total > 0 ? t('ui.dash.waiting', { count: total }) : doneText}
        </span>
      </div>
      <div className="dash-queue" style={{ '--n': Math.min(jobs.length, 5) } as CSSProperties}>
        {jobs.map((j) => {
          const body = (
            <>
              <b>
                {formatNumber(j.count, { locale })}
                <Icon name="chevron-right" size={16} />
              </b>
              <span>{j.label}</span>
              <small>{j.count === 0 ? t('ui.dash.no_backlog') : j.hint}</small>
            </>
          );
          return j.href ? (
            <a
              key={j.id}
              href={j.href}
              className="dash-job no-underline hover:no-underline"
              data-done={j.count === 0 ? 1 : 0}
            >
              {body}
            </a>
          ) : (
            <button
              key={j.id}
              type="button"
              className="dash-job"
              data-done={j.count === 0 ? 1 : 0}
              onClick={j.onClick}
            >
              {body}
            </button>
          );
        })}
      </div>
    </section>
  );
}

export type PipeRow = [label: string, count: number, tone: Tone];

/** Đơn đang chạy từ trái sang phải, rồi kết quả các đơn đã xong. */
export function Pipeline({ steps, outcomes }: { steps: PipeRow[]; outcomes: PipeRow[] }) {
  const { t, locale } = useT();
  const moving = steps.reduce((n, s) => n + s[1], 0);
  const ended = outcomes.reduce((n, s) => n + s[1], 0);
  const top = Math.max(...steps.map((s) => s[1]), 1);
  return (
    <div className="dash-pipe">
      <ol
        className="dash-pipe__steps"
        aria-label={t('ui.dash.moving', { count: moving })}
        style={{ '--n': steps.length } as CSSProperties}
      >
        {steps.map(([k, v, tone]) => (
          <li key={k} data-tone={tone}>
            <b>{formatNumber(v, { locale })}</b>
            <span>{k}</span>
            <i style={{ width: `${Math.max(4, (v / top) * 100)}%` }} />
          </li>
        ))}
      </ol>
      <div className="dash-pipe__out">
        <span className="dash-sub">{t('ui.dash.outcomes')}</span>
        {outcomes.map(([k, v, tone]) => (
          <span key={k} className="dash-pipe__chip" data-tone={tone}>
            <i aria-hidden="true" />
            {k} <b>{formatNumber(v, { locale })}</b>{' '}
            <small>{formatPercent(shareOf(v, ended) / 100, { locale, digits: 1 })}</small>
          </span>
        ))}
      </div>
    </div>
  );
}

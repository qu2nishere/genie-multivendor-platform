import { useT } from '@/shared/hooks/useT';
import { formatNumber, formatPercent, formatVND } from '@/shared/lib/format';
import { shareOf, type Tone } from './dash.math';

// BarList, StackBar, SegBar (plan G06.31).

/** Thanh xếp hạng: rows [[nhãn, giá trị]]; độ dài theo hàng lớn nhất, hàng dẫn đầu tô đậm. */
export function BarList({
  rows,
  format,
}: {
  rows: [string, number][];
  format?: (v: number) => string;
}) {
  const { locale } = useT();
  const fmt = format ?? ((v: number) => formatNumber(v, { locale }));
  const max = Math.max(...rows.map((r) => r[1]), 1);
  return (
    <ol className="dash-bars">
      {rows.map(([k, v], i) => (
        <li key={k} data-lead={i === 0 && v === max ? 1 : 0}>
          <div className="dash-bar__top">
            <span>{k}</span>
            <b>{fmt(v)}</b>
          </div>
          <div className="dash-bar__track">
            <div className="dash-bar__fill" style={{ width: `${(v / max) * 100}%` }} />
          </div>
        </li>
      ))}
    </ol>
  );
}

export interface StackPart {
  label: string;
  value: number;
  tone: Tone;
  note?: string;
  href?: string;
  onClick?: () => void;
}

/** Một thanh chia các phần của tổng (tiền giữ / khả dụng / chờ…), chú thích mang con số. */
export function StackBar({
  parts,
  label,
  format,
}: {
  parts: StackPart[];
  label: string;
  format?: (v: number) => string;
}) {
  const { locale } = useT();
  const fmt = format ?? ((v: number) => formatVND(v, { locale }));
  const total = parts.reduce((n, p) => n + p.value, 0);
  const pct = (v: number) => formatPercent(shareOf(v, total) / 100, { locale, digits: 1 });
  return (
    <div className="dash-stack">
      <div
        className="dash-stack__bar"
        role="img"
        aria-label={`${label}: ${parts.map((p) => `${p.label} ${fmt(p.value)}`).join(', ')}`}
      >
        {parts.map(
          (p) =>
            p.value > 0 && <i key={p.label} data-tone={p.tone} style={{ flexGrow: p.value }} />,
        )}
      </div>
      <ul className="dash-stack__legend">
        {parts.map((p) => {
          const inner = (
            <>
              <i data-tone={p.tone} aria-hidden="true" />
              <span>
                {p.label}
                {p.note && <small>{p.note}</small>}
              </span>
              <b>
                {fmt(p.value)}
                <small>{pct(p.value)}</small>
              </b>
            </>
          );
          return (
            <li key={p.label}>
              {p.href ? (
                <a href={p.href} className="dash-stack__item no-underline hover:no-underline">
                  {inner}
                </a>
              ) : p.onClick ? (
                <button type="button" className="dash-stack__item" onClick={p.onClick}>
                  {inner}
                </button>
              ) : (
                <div className="dash-stack__item">{inner}</div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}

/** Thanh cao chia khối, in tỉ lệ trên khối ≥ 9% (cảm xúc đánh giá, cơ cấu đơn). */
export function SegBar({
  parts,
  label,
  format,
}: {
  parts: { label: string; value: number; tone: Tone }[];
  label: string;
  format?: (v: number) => string;
}) {
  const { locale } = useT();
  const fmt = format ?? ((v: number) => formatNumber(v, { locale }));
  const total = parts.reduce((n, p) => n + p.value, 0);
  const share = (v: number) => formatPercent(shareOf(v, total) / 100, { locale, digits: 1 });
  return (
    <div className="dash-seg-bar">
      <div
        className="dash-seg-bar__bar"
        role="img"
        aria-label={`${label}: ${parts.map((p) => `${p.label} ${fmt(p.value)} (${share(p.value)})`).join(', ')}`}
      >
        {parts.map(
          (p) =>
            p.value > 0 && (
              <i
                key={p.label}
                data-tone={p.tone}
                style={{ flexGrow: p.value }}
                title={`${p.label}: ${fmt(p.value)} · ${share(p.value)}`}
              >
                {total > 0 && p.value / total >= 0.09 && share(p.value)}
              </i>
            ),
        )}
      </div>
      <ul className="dash-seg-bar__legend">
        {parts.map((p) => (
          <li key={p.label} data-tone={p.tone}>
            <i aria-hidden="true" />
            {p.label}
            <b>{fmt(p.value)}</b>
            <small>{share(p.value)}</small>
          </li>
        ))}
      </ul>
    </div>
  );
}

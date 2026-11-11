import { useEffect, useRef, useState, type KeyboardEvent, type PointerEvent } from 'react';
import { useT } from '@/shared/hooks/useT';
import { formatMoneyShort, formatVND } from '@/shared/lib/format';
import { axisTicks } from './dash.math';

export interface TimePoint {
  label: string;
  value: number;
}

export interface TimeChartProps {
  data: TimePoint[];
  /** bar: từng kỳ riêng; line: xu hướng nối tiếp. */
  kind?: 'bar' | 'line';
  /** Tên chuỗi số (dùng cho mô tả trình đọc màn hình + bảng). */
  name: string;
  format?: (v: number) => string;
  axis?: (v: number) => string;
  height?: number;
}

const PL = 58;
const PR = 10;
const PT = 26;
const PB = 26;

/**
 * Biểu đồ thời gian SVG (prototype dash.jsx TimeChart) — G06.32: trục chia "đẹp" (niceStep), con trỏ
 * dọc + nhãn nổi, phím ← → đọc từng mốc, Esc bỏ chọn, mô tả cho trình đọc màn hình và "Xem dạng bảng".
 * Xám là chính; chỉ kỳ cuối (đang xét) được tô màu.
 */
export function TimeChart({
  data,
  kind = 'bar',
  name,
  format,
  axis,
  height = 240,
}: TimeChartProps) {
  const { t, locale } = useT();
  const fmt = format ?? ((v: number) => formatVND(v, { locale }));
  const ax = axis ?? ((v: number) => formatMoneyShort(v, { locale }));
  const box = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(680);
  const [active, setActive] = useState<number | null>(null);

  useEffect(() => {
    const el = box.current;
    if (!el || typeof ResizeObserver === 'undefined') return undefined;
    const ro = new ResizeObserver((e) => setWidth(Math.max(240, e[0].contentRect.width)));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);
  useEffect(() => setActive(null), [data]);

  if (data.length === 0) {
    return <div className="dash-empty">{t('common.state.empty')}</div>;
  }

  const plotW = width - PL - PR;
  const plotH = height - PT - PB;
  const base = PT + plotH;
  const n = data.length;
  const max = Math.max(...data.map((d) => d.value), 1);
  const { top, ticks } = axisTicks(max, 4);
  const slot = plotW / n;
  const x = (i: number) =>
    kind === 'bar' ? PL + (i + 0.5) * slot : PL + (n > 1 ? (i / (n - 1)) * plotW : plotW / 2);
  const y = (v: number) => PT + (1 - v / top) * plotH;
  const every = Math.max(1, Math.ceil(n / Math.max(2, Math.floor(plotW / 62))));
  const peak = data.findIndex((d) => d.value === max);
  const barW = Math.min(24, Math.max(4, slot * 0.62));
  const line = data
    .map((d, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)} ${y(d.value).toFixed(1)}`)
    .join(' ');

  const pick = (e: PointerEvent<SVGSVGElement>) => {
    const r = e.currentTarget.getBoundingClientRect();
    const px = e.clientX - r.left;
    const i =
      kind === 'bar' ? Math.floor((px - PL) / slot) : Math.round(((px - PL) / plotW) * (n - 1));
    setActive(Math.min(n - 1, Math.max(0, i)));
  };
  const keys = (e: KeyboardEvent<SVGSVGElement>) => {
    if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      e.preventDefault();
      const d = e.key === 'ArrowRight' ? 1 : -1;
      setActive((a) => Math.min(n - 1, Math.max(0, (a === null ? (d > 0 ? -1 : n) : a) + d)));
    }
    if (e.key === 'Escape') setActive(null);
  };
  const tipX = active === null ? 0 : Math.min(width - 72, Math.max(72, x(active)));
  const focus = active === null ? n - 1 : active;

  return (
    <div>
      <div className="dash-chart" ref={box}>
        <svg
          width={width}
          height={height}
          viewBox={`0 0 ${width} ${height}`}
          role="img"
          tabIndex={0}
          aria-label={t('ui.dash.chart_aria', { name, max: fmt(max), at: data[peak].label })}
          onPointerMove={pick}
          onPointerDown={pick}
          onPointerLeave={() => setActive(null)}
          onKeyDown={keys}
          onBlur={() => setActive(null)}
          className="focus-visible:outline-none focus-visible:shadow-focus"
        >
          {ticks.map((tk) => (
            <g key={tk}>
              <line
                x1={PL}
                x2={width - PR}
                y1={y(tk)}
                y2={y(tk)}
                stroke={tk === 0 ? 'var(--gray-300)' : 'var(--gray-100)'}
              />
              <text x={PL - 8} y={y(tk) + 4} textAnchor="end">
                {ax(tk)}
              </text>
            </g>
          ))}
          {data.map(
            (d, i) =>
              (n - 1 - i) % every === 0 && (
                <text
                  key={`x${i}`}
                  x={x(i)}
                  y={height - 6}
                  textAnchor={x(i) > width - 24 ? 'end' : 'middle'}
                >
                  {d.label}
                </text>
              ),
          )}
          {kind === 'bar' &&
            data.map((d, i) => {
              const h = Math.max(d.value ? 2 : 0, base - y(d.value));
              const bx = x(i) - barW / 2;
              const r = Math.min(4, h);
              return (
                <path
                  key={i}
                  d={`M${bx} ${base}V${base - h + r}q0 ${-r} ${r} ${-r}H${bx + barW - r}q${r} 0 ${r} ${r}V${base}Z`}
                  fill={
                    active === i
                      ? 'var(--brand-900)'
                      : i === n - 1
                        ? 'var(--brand-700)'
                        : 'var(--gray-300)'
                  }
                />
              );
            })}
          {kind === 'line' && (
            <>
              <path
                d={`${line} L${x(n - 1)} ${base} L${x(0)} ${base} Z`}
                fill="var(--gray-400)"
                fillOpacity=".12"
              />
              <path
                d={line}
                fill="none"
                stroke="var(--ink-700)"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              {active !== null && (
                <line x1={x(active)} x2={x(active)} y1={PT} y2={base} stroke="var(--gray-300)" />
              )}
              <circle
                cx={x(focus)}
                cy={y(data[focus].value)}
                r="6"
                fill="var(--color-accent)"
                stroke="var(--brand-900)"
                strokeWidth="2"
              />
            </>
          )}
          {active === null && (
            <text
              className="dash-chart__peak"
              x={Math.min(width - PR - 30, Math.max(PL + 30, x(peak)))}
              y={y(max) - 8}
              textAnchor="middle"
            >
              {ax(max)}
            </text>
          )}
        </svg>
        {active !== null && (
          <div className="dash-tip" style={{ left: tipX }} aria-live="polite">
            <b>{fmt(data[active].value)}</b>
            {data[active].label}
          </div>
        )}
      </div>
      <details className="dash-table-toggle">
        <summary>{t('ui.dash.as_table')}</summary>
        <table>
          <thead>
            <tr>
              <th scope="col">{t('ui.dash.point')}</th>
              <th scope="col">{name}</th>
            </tr>
          </thead>
          <tbody>
            {data.map((d) => (
              <tr key={d.label}>
                <td>{d.label}</td>
                <td>{fmt(d.value)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  );
}

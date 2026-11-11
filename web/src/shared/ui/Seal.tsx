import { useMemo } from 'react';
import { useT } from '@/shared/hooks/useT';

export interface SealProps {
  /** Mã shop — sinh hoa văn riêng, không shop nào trùng. */
  id: string | number;
  name: string;
  size?: number;
}

/** Hoa văn đường nét (rosette) suy từ mã shop (prototype Shared.jsx Seal). Thuần, có test. */
export function sealCurves(id: string | number): string[] {
  let h = 7;
  for (const c of String(id)) h = (h * 31 + c.charCodeAt(0)) % 9973;
  const k = 5 + (h % 7);
  const turn = ((h % 90) * Math.PI) / 180;
  return [1, 0.8, 0.62].map((scale, n) => {
    let d = '';
    for (let i = 0; i <= 180; i++) {
      const t = (i / 180) * 2 * Math.PI;
      const r = 44 * scale * (0.72 + 0.28 * Math.cos(k * t + n * 0.9));
      d +=
        (i ? 'L' : 'M') +
        (50 + r * Math.cos(t + turn)).toFixed(1) +
        ' ' +
        (50 + r * Math.sin(t + turn)).toFixed(1);
    }
    return d + 'Z';
  });
}

/** Con dấu shop đã xác minh (G06.24). Cỡ < 28px chỉ vẽ một vòng, nét dày. */
export function Seal({ id, name, size = 40 }: SealProps) {
  const { t } = useT();
  const curves = useMemo(() => sealCurves(id), [id]);
  const small = size < 28;
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 100"
      role="img"
      aria-label={t('ui.seal.label', { name })}
      className="flex-none"
    >
      <circle
        cx="50"
        cy="50"
        r="49"
        fill="#fff"
        stroke="var(--brand-500)"
        strokeWidth={small ? 5 : 2}
      />
      {curves.slice(0, small ? 1 : 3).map((d) => (
        <path key={d} d={d} fill="none" stroke="var(--brand-500)" strokeWidth={small ? 5 : 1.2} />
      ))}
      {!small && (
        <>
          <circle cx="50" cy="50" r="19" fill="var(--brand-500)" />
          <text
            x="50"
            y="58"
            textAnchor="middle"
            fontSize="23"
            fontWeight="800"
            fill="#fff"
            fontFamily="var(--font-sans)"
          >
            {name.trim()[0] ?? '?'}
          </text>
        </>
      )}
    </svg>
  );
}

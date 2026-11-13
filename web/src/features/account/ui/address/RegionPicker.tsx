// Bộ chọn Tỉnh → Quận → Phường kiểu GHN (prototype AddressModal.jsx RegionPicker) — plan G10.49:
// nút mở bảng 3 tab + ô tìm nhanh (lọc phía client), chọn xong cấp nào tự sang cấp sau.
import { useCallback, useRef, useState } from 'react';
import { useClickOutside } from '@/shared/hooks/useClickOutside';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon } from '@/shared/ui';
import { useDistricts, useProvinces, useWards } from '../../application/useAddresses';
import type { Region, Ward } from '../../domain/address.types';

export interface RegionValue {
  province: Region | null;
  district: Region | null;
  ward: Ward | null;
}

export interface RegionPickerProps {
  value: RegionValue;
  onChange: (v: RegionValue) => void;
  invalid?: boolean;
  id?: string;
  describedBy?: string;
}

type Level = 'province' | 'district' | 'ward';

export function RegionPicker({ value, onChange, invalid, id, describedBy }: RegionPickerProps) {
  const { t } = useT();
  const [open, setOpen] = useState(false);
  const [level, setLevel] = useState<Level>('province');
  const [q, setQ] = useState('');
  const ref = useRef<HTMLDivElement>(null);
  const close = useCallback(() => setOpen(false), []);
  useClickOutside(ref, close, open);

  const provinces = useProvinces(open);
  const districts = useDistricts(value.province?.id ?? null);
  const wards = useWards(value.district?.id ?? null);

  const openPanel = () => {
    setOpen(true);
    setLevel(value.province ? (value.district ? 'ward' : 'district') : 'province');
    setQ('');
  };

  const options: { key: string; name: string; pick: () => void; on: boolean }[] =
    level === 'province'
      ? (provinces.data ?? []).map((p) => ({
          key: String(p.id),
          name: p.name,
          on: p.id === value.province?.id,
          pick: () => {
            onChange({ province: p, district: null, ward: null });
            setLevel('district');
          },
        }))
      : level === 'district'
        ? (districts.data ?? []).map((d) => ({
            key: String(d.id),
            name: d.name,
            on: d.id === value.district?.id,
            pick: () => {
              onChange({ province: value.province, district: d, ward: null });
              setLevel('ward');
            },
          }))
        : (wards.data ?? []).map((w) => ({
            key: w.code,
            name: w.name,
            on: w.code === value.ward?.code,
            pick: () => {
              onChange({ ...value, ward: w });
              setOpen(false);
            },
          }));
  const shown = options.filter((o) => o.name.toLowerCase().includes(q.trim().toLowerCase()));
  const display = [value.ward?.name, value.district?.name, value.province?.name]
    .filter(Boolean)
    .join(', ');
  const tabs: { id: Level; label: string; enabled: boolean }[] = [
    {
      id: 'province',
      label: value.province?.name ?? t('account.address.region.province'),
      enabled: true,
    },
    {
      id: 'district',
      label: value.district?.name ?? t('account.address.region.district'),
      enabled: !!value.province,
    },
    {
      id: 'ward',
      label: value.ward?.name ?? t('account.address.region.ward'),
      enabled: !!value.district,
    },
  ];

  return (
    <div ref={ref} className="relative" onKeyDown={(e) => e.key === 'Escape' && setOpen(false)}>
      <button
        type="button"
        id={id}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-invalid={invalid || undefined}
        aria-describedby={describedBy}
        onClick={() => (open ? setOpen(false) : openPanel())}
        className={cn(
          'w-full flex items-center gap-2.5 h-11 px-3 rounded-md bg-surface-card text-left cursor-pointer',
          'border-[1.5px] border-solid focus-visible:outline-none focus-visible:shadow-focus',
          invalid ? 'border-red-500' : open ? 'border-primary' : 'border-line',
        )}
      >
        <Icon name="map-pin" size={17} className="text-subtle flex-none" />
        <span className={cn('flex-1 truncate text-base', display ? 'text-strong' : 'text-subtle')}>
          {display || t('account.address.region.placeholder')}
        </span>
        <Icon
          name="chevron-down"
          size={18}
          className={cn('text-subtle flex-none transition-transform', open && 'rotate-180')}
        />
      </button>

      {open && (
        <div
          role="dialog"
          aria-label={t('account.address.region.label')}
          className="absolute top-[calc(100%+6px)] inset-x-0 z-20 bg-surface-card border border-solid border-line rounded-md shadow-lg overflow-hidden"
        >
          <div role="tablist" className="flex border-0 border-b border-solid border-line-subtle">
            {tabs.map((tb) => (
              <button
                key={tb.id}
                type="button"
                role="tab"
                aria-selected={level === tb.id}
                disabled={!tb.enabled}
                onClick={() => {
                  setLevel(tb.id);
                  setQ('');
                }}
                className={cn(
                  'flex-1 min-w-0 h-10 px-2 border-0 border-b-2 border-solid bg-transparent truncate text-[12.5px] cursor-pointer',
                  'disabled:cursor-not-allowed disabled:text-disabled',
                  level === tb.id
                    ? 'border-primary text-brand-600 font-semibold'
                    : 'border-transparent text-body font-medium',
                )}
              >
                {tb.label}
              </button>
            ))}
          </div>
          <div className="px-2.5 py-2">
            <label className="flex items-center gap-[7px] h-9 border border-solid border-line rounded-sm px-2.5 bg-surface-sunken">
              <Icon name="search" size={15} className="text-subtle" />
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder={t('account.address.region.search')}
                aria-label={t('account.address.region.search')}
                autoFocus
                className="flex-1 border-0 bg-transparent outline-none text-sm"
              />
            </label>
          </div>
          <ul className="list-none m-0 p-0 pb-1.5 max-h-[210px] overflow-y-auto">
            {shown.length === 0 ? (
              <li className="p-5 text-center text-sm text-subtle">
                {t('account.address.region.none')}
              </li>
            ) : (
              shown.map((o) => (
                <li key={o.key}>
                  <button
                    type="button"
                    onClick={o.pick}
                    className={cn(
                      'flex items-center gap-2 w-full border-0 bg-transparent px-3.5 py-[9px] text-left text-sm cursor-pointer hover:bg-surface-sunken',
                      o.on ? 'text-brand-600' : 'text-body',
                    )}
                  >
                    <span className="flex-1">{o.name}</span>
                    {o.on && <Icon name="check" size={15} />}
                  </button>
                </li>
              ))
            )}
          </ul>
        </div>
      )}
    </div>
  );
}

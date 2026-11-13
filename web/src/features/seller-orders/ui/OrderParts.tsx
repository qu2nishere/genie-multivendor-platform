// Phần dùng chung của màn Đơn hàng / Trả hàng (prototype Fulfillment.jsx): thanh tab (tab "Trả hàng" là
// route /seller/returns), mốc thời gian tương đối, dòng khóa–giá trị.
import { useEffect, useRef, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Tabs } from '@/shared/ui';
import { sellerNow } from '../application/useSellerOrders';
import { whenLabel } from '../domain/order.rules';
import type { OrderTab } from '../domain/order.types';

export type TabId = OrderTab | 'RETURN';
const TABS: TabId[] = [
  'ALL',
  'NEW',
  'TO_PREPARE',
  'DELIVERING',
  'DELIVERED',
  'RETURN',
  'CANCELLED',
];

export function OrderTabs({
  value,
  counts,
}: {
  value: TabId;
  counts?: Partial<Record<OrderTab, number>>;
}) {
  const { t } = useT();
  const navigate = useNavigate();
  const box = useRef<HTMLDivElement>(null);
  // Màn hẹp: dải tab cuộn ngang — đưa tab đang chọn vào khung nhìn
  useEffect(() => {
    const el = box.current?.querySelector<HTMLElement>('[aria-selected="true"]');
    const bar = el?.parentElement;
    if (el && bar && bar.scrollWidth > bar.clientWidth)
      bar.scrollLeft = el.offsetLeft - (bar.clientWidth - el.offsetWidth) / 2;
  }, [value]);
  return (
    <div ref={box}>
      <Tabs<TabId>
        aria-label={t('seller-orders.tabs.label')}
        value={value}
        onChange={(id) =>
          navigate(id === 'RETURN' ? '/returns' : id === 'ALL' ? '/orders' : `/orders?tab=${id}`)
        }
        items={TABS.map((id) => ({
          id,
          label: t(`seller-orders.tabs.${id}`),
          badge: (id === 'NEW' || id === 'TO_PREPARE') && counts?.[id] ? counts[id] : undefined,
        }))}
      />
    </div>
  );
}

export function When({ iso }: { iso: string }) {
  const { t } = useT();
  const w = whenLabel(iso, sellerNow());
  if (w.kind === 'today') return <>{t('seller-orders.when.today', { time: w.time })}</>;
  if (w.kind === 'yesterday') return <>{t('seller-orders.when.yesterday')}</>;
  if (w.kind === 'days') return <>{t('seller-orders.when.days', { count: w.days })}</>;
  return <>{`${iso.slice(8, 10)}/${iso.slice(5, 7)}/${iso.slice(0, 4)}`}</>;
}

export function Row({ k, v, strong }: { k: ReactNode; v: ReactNode; strong?: boolean }) {
  return (
    <div className="flex justify-between gap-3 py-1 text-sm">
      <span className="text-muted">{k}</span>
      <span
        className={cn(
          'tabular-nums text-right',
          strong ? 'text-price font-bold' : 'text-body font-medium',
        )}
      >
        {v}
      </span>
    </div>
  );
}

export const labelCls = 'block font-semibold text-sm text-strong mb-1.5';

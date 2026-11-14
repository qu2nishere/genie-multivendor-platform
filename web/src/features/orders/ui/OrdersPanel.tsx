// "Đơn mua" (prototype Account.jsx OrdersPanel / OrderGroup / OrderCard) — plan G10.32, island client:load.
// Tab theo status_group (liên kết thật ?tab= — R16), thẻ đơn có chip "Genie giữ tiền", phương thức,
// trạng thái; đơn online cùng phiên chờ thanh toán / đã hủy gộp thành "Nhóm đơn hàng".
import { MoneyChip } from '@/features/payment';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import {
  Badge,
  Button,
  EmptyState,
  Icon,
  PanelHead,
  Seal,
  Skeleton,
  StatusBadge,
  Tabs,
  VendorChip,
} from '@/shared/ui';
import { useOrders } from '../application/useOrders';
import { groupOrders, statusTone, STATUS_GROUPS, sumOrders } from '../domain/order.rules';
import type { OrderItem, OrderSummary, StatusGroup } from '../domain/order.types';
import {
  OrderActionButtons,
  orderHref,
  useOrderHandlers,
  type OrderHandlers,
} from './OrderActions';

export interface OrdersPanelProps {
  tab: StatusGroup | null;
}

const productHref = (it: OrderItem) => `/p/${it.productSlug}-${it.productId}`;

function ShopHead({ o }: { o: OrderSummary }) {
  return (
    <span className="tr-chip">
      <Seal id={o.shop.id} name={o.shop.name} size={30} />
      <VendorChip name={o.shop.name} authentic={o.shop.authentic} href={`/shop/${o.shop.id}`} />
    </span>
  );
}

export function OrderStatus({ o }: { o: Pick<OrderSummary, 'status'> }) {
  const { t } = useT();
  return <StatusBadge status={statusTone(o.status)} label={t(`orders.status.${o.status}`)} />;
}

function ItemRow({ it, big }: { it: OrderItem; big?: boolean }) {
  const { t, locale } = useT();
  const size = big ? 60 : 50;
  return (
    <a
      href={productHref(it)}
      className="flex gap-3 py-3 no-underline text-[inherit] hover:bg-gray-25 border-0 border-b border-solid border-line-subtle last:border-b-0"
    >
      <img
        src={it.image}
        alt=""
        width={size}
        height={size}
        loading="lazy"
        className="rounded-sm object-cover flex-none border border-solid border-line-subtle"
        style={{ width: size, height: size }}
      />
      <span className="flex-1 min-w-0">
        <span className="block text-sm text-body line-clamp-1">{it.title}</span>
        <span className="block text-xs text-muted mt-[3px]">
          {t('cart.line.variant', { variant: it.variantLabel })}
        </span>
        <span className="block text-xs text-muted">x{it.qty}</span>
      </span>
      <span className="font-semibold text-sm text-price flex-none tabular-nums">
        {formatVND(it.price, { locale })}
      </span>
    </a>
  );
}

function OrderCard({ o, h }: { o: OrderSummary; h: OrderHandlers }) {
  const { t, locale } = useT();
  return (
    <article
      className="bg-surface-card border border-solid border-line-subtle rounded-lg overflow-hidden"
      aria-label={t('orders.card.label', { code: o.code })}
    >
      <header className="flex items-center gap-2.5 flex-wrap px-4 py-[11px] border-0 border-b border-solid border-line-subtle">
        <ShopHead o={o} />
        <a
          href={orderHref(o.id)}
          className="font-mono text-xs text-subtle no-underline hover:underline"
        >
          {o.code}
        </a>
        <span className="ml-auto flex items-center gap-2 flex-wrap justify-end">
          <MoneyChip status={o.status} method={o.paymentMethod} />
          {o.deliveredAt && o.status === 'DELIVERED' && (
            <span className="text-xs text-mint-600 inline-flex items-center gap-1">
              <Icon name="truck" size={14} />
              {t('orders.card.delivered')}
            </span>
          )}
          <Badge variant="neutral">{t(`orders.pay.${o.paymentMethod}`)}</Badge>
          <OrderStatus o={o} />
        </span>
      </header>
      <div className="px-4 py-1.5">
        {o.items.map((it) => (
          <ItemRow key={it.id} it={it} big />
        ))}
      </div>
      <footer className="flex items-center flex-wrap gap-x-3 gap-y-2.5 px-4 py-3 bg-brand-50">
        <span className="text-sm text-muted">{t('orders.card.total')}</span>
        <span className="font-extrabold text-xl text-price mr-auto tabular-nums">
          {formatVND(o.grandTotal, { locale })}
        </span>
        <div className="flex items-center gap-2 flex-wrap">
          <OrderActionButtons o={o} h={h} />
        </div>
      </footer>
    </article>
  );
}

function OrderGroupCard({ orders, h }: { orders: OrderSummary[]; h: OrderHandlers }) {
  const { t, locale } = useT();
  const vnd = (n: number) => formatVND(n, { locale });
  const cancelled = orders[0].status === 'CANCELLED';
  return (
    <article className="bg-surface-card border border-solid border-line-subtle rounded-lg overflow-hidden">
      <header className="flex items-center gap-2.5 px-4 py-[11px] bg-surface-sunken border-0 border-b border-solid border-line-subtle">
        <Icon name="package" size={16} className="text-muted" />
        <span className="font-semibold text-sm text-strong">
          {t(cancelled ? 'orders.group.cancelled' : 'orders.group.pending')}
        </span>
        <span className="ml-auto">
          <OrderStatus o={orders[0]} />
        </span>
      </header>
      {orders.map((o, i) => (
        <section
          key={o.id}
          className={
            i < orders.length - 1 ? 'border-0 border-b border-dashed border-line-subtle' : ''
          }
        >
          <div className="flex items-center gap-2.5 px-4 py-[11px]">
            <ShopHead o={o} />
            <a
              href={orderHref(o.id)}
              className="font-mono text-xs text-subtle ml-auto no-underline"
            >
              {o.code}
            </a>
          </div>
          <div className="px-4 pb-3">
            {o.items.map((it) => (
              <ItemRow key={it.id} it={it} />
            ))}
          </div>
        </section>
      ))}
      {!cancelled && (
        <dl className="m-0 px-4 py-2 bg-surface-sunken border-0 border-t border-solid border-line-subtle flex flex-col gap-[3px] text-xs text-muted">
          <div className="flex justify-between">
            <dt>{t('orders.group.goods')}</dt>
            <dd className="m-0 tabular-nums">{vnd(sumOrders(orders, 'subtotal'))}</dd>
          </div>
          <div className="flex justify-between">
            <dt>{t('orders.group.shipping')}</dt>
            <dd className="m-0 tabular-nums">+{vnd(sumOrders(orders, 'shippingFee'))}</dd>
          </div>
          {sumOrders(orders, 'discountTotal') > 0 && (
            <div className="flex justify-between text-mint-600">
              <dt>{t('orders.group.discount')}</dt>
              <dd className="m-0 tabular-nums">-{vnd(sumOrders(orders, 'discountTotal'))}</dd>
            </div>
          )}
        </dl>
      )}
      <footer className="flex items-center flex-wrap gap-x-3 gap-y-2.5 px-4 py-3 bg-brand-50">
        <span className="text-sm text-muted">
          {t('orders.group.total', { count: orders.length })}
        </span>
        <span className="font-extrabold text-xl text-price mr-auto tabular-nums">
          {vnd(sumOrders(orders, 'grandTotal'))}
        </span>
        {cancelled ? (
          <Button size="sm" onClick={() => h.again(orders.flatMap((o) => o.items))}>
            {t('orders.group.again_all')}
          </Button>
        ) : (
          <>
            <Button
              size="sm"
              onClick={() => orders[0].sessionId != null && h.pay(orders[0].sessionId)}
            >
              {t('orders.action.pay')}
            </Button>
            <Button size="sm" variant="ghost" onClick={() => h.cancel(orders)}>
              {t('orders.group.cancel')}
            </Button>
          </>
        )}
      </footer>
    </article>
  );
}

function Panel({ tab }: OrdersPanelProps) {
  const { t } = useT();
  const q = useOrders(tab ?? undefined);
  const { handlers, dialogs } = useOrderHandlers();
  const tabs = [
    { id: 'ALL', label: t('common.action.all'), href: '/orders' },
    ...STATUS_GROUPS.map((g) => ({
      id: g,
      label: t(`orders.tab.${g}`),
      href: `/orders?tab=${g.toLowerCase()}`,
    })),
  ];
  const rows = groupOrders(q.data?.items ?? []);

  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
      aria-labelledby="orders-title"
    >
      <div className="px-[22px] pt-4 max-sm:px-3">
        <PanelHead icon="package" title={t('orders.title')} id="orders-title" level={2} />
        <Tabs items={tabs} value={tab ?? 'ALL'} aria-label={t('orders.title')} />
      </div>
      <div className="px-[22px] pt-4 pb-[22px] flex flex-col gap-3.5 bg-surface-sunken max-sm:px-3">
        {q.isPending ? (
          [0, 1].map((i) => <Skeleton key={i} className="h-[180px]" rounded="lg" />)
        ) : rows.length === 0 ? (
          <EmptyState icon="package" text={t('orders.empty')} />
        ) : (
          rows.map((r) =>
            r.kind === 'group' ? (
              <OrderGroupCard
                key={`g${r.sessionId}-${r.orders[0].status}`}
                orders={r.orders}
                h={handlers}
              />
            ) : (
              <OrderCard key={r.order.id} o={r.order} h={handlers} />
            ),
          )
        )}
      </div>
      {dialogs}
    </section>
  );
}

export default function OrdersPanel(props: OrdersPanelProps) {
  return (
    <Providers>
      <Panel {...props} />
    </Providers>
  );
}

// Đơn hàng của shop (prototype Fulfillment.jsx) — plan G11.15–G11.17: tab trạng thái (+ badge Mới / Chờ
// chuẩn bị), tìm mã đơn / khách, sắp xếp, thẻ đơn (sản phẩm, lời nhắn, lý do từ chối), Xác nhận / Từ chối /
// Chuẩn bị hàng / Sẵn sàng giao ĐVVC / In lại vận đơn, Chi tiết. ?tab=&q= giữ trên URL.
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import {
  Avatar,
  Badge,
  Button,
  EmptyState,
  Icon,
  Pagination,
  Select,
  Skeleton,
  StatusBadge,
  toast,
} from '@/shared/ui';
import { useConfirmOrder, usePrepareOrder } from '../application/useFulfillment';
import { useSellerOrders } from '../application/useSellerOrders';
import { allowedActions, CARRIER_NAME, PAYMENT_NAME } from '../domain/fulfillment.rules';
import { orderBadge } from '../domain/order.rules';
import type { OrderTab, SellerOrder } from '../domain/order.types';
import { DetailModal, RejectModal, RouteModal, WaybillModal } from './OrderModals';
import { OrderTabs } from './OrderParts';

const TAB_IDS: OrderTab[] = ['ALL', 'NEW', 'TO_PREPARE', 'DELIVERING', 'DELIVERED', 'CANCELLED'];
const isTab = (v: string | null): v is OrderTab => !!v && (TAB_IDS as string[]).includes(v);
const PAGE_SIZE = 10;

function OrderCard({
  o,
  onDetail,
  onReject,
  onRoute,
  onWaybill,
}: {
  o: SellerOrder;
  onDetail: () => void;
  onReject: () => void;
  onRoute: () => void;
  onWaybill: () => void;
}) {
  const { t, locale } = useT();
  const confirm = useConfirmOrder();
  const prepare = usePrepareOrder();
  const b = orderBadge(o);
  const actions = allowedActions(o);
  const fail = (e: unknown) => toast.error(errorMessage(e, t));
  return (
    <article className="bg-surface-card rounded-lg shadow-sm overflow-hidden">
      <div className="flex items-center gap-2.5 px-[18px] py-3.5 border-0 border-b border-solid border-line-subtle bg-gray-25 flex-wrap">
        <span className="code text-[13px] font-semibold text-strong">{o.code}</span>
        <StatusBadge status={b.tone} label={t(b.key)} />
        <Badge variant="neutral">{PAYMENT_NAME[o.payment]}</Badge>
        <span className="ml-auto flex items-center gap-2 max-md:ml-0">
          <Avatar name={o.customer.name} src={o.customer.avatarUrl} size={26} />
          <span className="text-sm text-body">
            {o.customer.name} · {o.shipTo}
          </span>
        </span>
      </div>
      <div className="px-[18px] py-3.5">
        {o.items.map((i, k) => (
          <div key={k} className="flex items-center gap-2.5 py-1.5 text-sm text-body">
            <Icon name="package" size={15} className="text-subtle flex-none" />
            <span className="flex-1 min-w-0">
              {i.name} <span className="text-muted">· {i.variant}</span>
            </span>
            <span className="text-muted">x{i.quantity}</span>
            <span className="w-[110px] text-right tabular-nums font-semibold max-md:w-auto">
              {formatVND(i.unitPrice, { locale })}
            </span>
          </div>
        ))}
        {o.note && (
          <div className="flex gap-2 mt-2 px-3 py-2 bg-amber-50 rounded-md text-sm text-body">
            <Icon name="message-square" size={15} className="text-amber-600 flex-none mt-0.5" />
            <span>
              <b>{t('seller-orders.card.note')}:</b> {o.note}
            </span>
          </div>
        )}
        {o.rejectReason && (
          <div className="mt-2 text-xs text-red-600">
            {t('seller-orders.card.rejected', { reason: o.rejectReason })}
          </div>
        )}
      </div>
      <div className="flex items-center gap-2.5 px-[18px] py-3 border-0 border-t border-solid border-line-subtle flex-wrap">
        <Button variant="ghost" size="sm" iconLeft="file-text" onClick={onDetail}>
          {t('seller-orders.card.detail')}
        </Button>
        {o.shipment?.trackingCode ? (
          <>
            <Badge variant="mint" icon="truck">
              {CARRIER_NAME[o.shipment.carrier]}
            </Badge>
            <span className="code text-[13px] text-body">
              {t('seller-orders.card.waybill', { code: o.shipment.trackingCode })}
            </span>
            {actions.includes('PRINT_LABEL') && (
              <Button
                variant="ghost"
                size="sm"
                iconLeft="printer"
                className="ml-auto"
                onClick={onWaybill}
              >
                {t('seller-orders.card.reprint')}
              </Button>
            )}
          </>
        ) : (
          <>
            <span className="text-sm text-muted">
              {t('seller-orders.card.total')}{' '}
              <b className="text-price">{formatVND(o.total, { locale })}</b>
            </span>
            <span className="ml-auto flex gap-2">
              {actions.includes('REJECT') && (
                <Button size="sm" variant="ghost" className="!text-red-600" onClick={onReject}>
                  {t('seller-orders.card.reject')}
                </Button>
              )}
              {actions.includes('CONFIRM') && (
                <Button
                  size="sm"
                  iconLeft="check"
                  loading={confirm.isPending}
                  onClick={() =>
                    confirm.mutate(o.id, {
                      onSuccess: () => toast.success(t('seller-orders.card.confirmed')),
                      onError: fail,
                    })
                  }
                >
                  {t('seller-orders.card.confirm')}
                </Button>
              )}
              {actions.includes('PREPARE') && (
                <Button
                  size="sm"
                  iconLeft="package"
                  loading={prepare.isPending}
                  onClick={() =>
                    prepare.mutate(o.id, {
                      onSuccess: () => toast.success(t('seller-orders.card.prepared')),
                      onError: fail,
                    })
                  }
                >
                  {t('seller-orders.card.prepare')}
                </Button>
              )}
              {actions.includes('HAND_OVER') && (
                <Button size="sm" iconLeft="truck" onClick={onRoute}>
                  {t('seller-orders.card.hand_over')}
                </Button>
              )}
            </span>
          </>
        )}
      </div>
    </article>
  );
}

export default function FulfillmentPage() {
  const { t } = useT();
  const [params, setParams] = useSearchParams();
  const tabParam = params.get('tab');
  const tab: OrderTab = isTab(tabParam) ? tabParam : 'ALL';
  const [q, setQ] = useState(params.get('q') ?? '');
  const [term, setTerm] = useState(q.trim());
  const [sort, setSort] = useState<'NEWEST' | 'OLDEST'>('NEWEST');
  const [page, setPage] = useState(1);
  const [detail, setDetail] = useState<number | null>(null);
  const [reject, setReject] = useState<SellerOrder | null>(null);
  const [route, setRoute] = useState<SellerOrder | null>(null);
  const [waybill, setWaybill] = useState<number | null>(null);

  // Tìm có trễ 250 ms; ghi lại ?q= để bấm "Chi tiết" ở Tổng quan / tìm nhanh mở đúng đơn
  useEffect(() => {
    const id = setTimeout(() => {
      setTerm(q.trim());
      setPage(1);
      setParams(
        (p) => {
          const n = new URLSearchParams(p);
          if (q.trim()) n.set('q', q.trim());
          else n.delete('q');
          return n;
        },
        { replace: true },
      );
    }, 250);
    return () => clearTimeout(id);
  }, [q, setParams]);
  useEffect(() => setPage(1), [tab]);

  const list = useSellerOrders({ tab, q: term, sort, page, size: PAGE_SIZE });
  const data = list.data;

  return (
    <div className="max-w-[1180px]">
      <OrderTabs value={tab} counts={data?.counts} />
      <div className="flex items-center gap-3 mt-3.5 flex-wrap">
        <div className="relative flex-1 max-w-[360px] min-w-[200px]">
          <Icon
            name="search"
            size={16}
            className="absolute left-3 top-3 text-subtle pointer-events-none"
          />
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            aria-label={t('seller-orders.search')}
            placeholder={t('seller-orders.search_placeholder')}
            className="w-full h-10 pl-9 pr-3 border-[1.5px] border-solid border-line rounded-md text-sm bg-surface-card text-body outline-none focus:border-primary"
          />
        </div>
        <div className="w-[150px] ml-auto">
          <Select
            aria-label={t('seller-orders.sort.label')}
            value={sort}
            onChange={setSort}
            options={[
              { value: 'NEWEST', label: t('seller-orders.sort.NEWEST') },
              { value: 'OLDEST', label: t('seller-orders.sort.OLDEST') },
            ]}
          />
        </div>
      </div>
      <div className="mt-4 flex flex-col gap-3.5">
        {!data ? (
          <Skeleton lines={8} />
        ) : data.items.length === 0 ? (
          <div className="bg-surface-card rounded-lg py-14">
            <EmptyState icon="package" text={t('seller-orders.empty')} />
          </div>
        ) : (
          data.items.map((o) => (
            <OrderCard
              key={o.id}
              o={o}
              onDetail={() => setDetail(o.id)}
              onReject={() => setReject(o)}
              onRoute={() => setRoute(o)}
              onWaybill={() => setWaybill(o.id)}
            />
          ))
        )}
        {data && data.total_pages > 1 && (
          <Pagination page={page} total={data.total_pages} onChange={setPage} />
        )}
      </div>

      <RejectModal order={reject} onClose={() => setReject(null)} />
      <DetailModal id={detail} onClose={() => setDetail(null)} />
      <RouteModal
        order={route}
        onClose={() => setRoute(null)}
        onDone={(id) => {
          setRoute(null);
          setWaybill(id);
        }}
      />
      <WaybillModal id={waybill} onClose={() => setWaybill(null)} />
    </div>
  );
}

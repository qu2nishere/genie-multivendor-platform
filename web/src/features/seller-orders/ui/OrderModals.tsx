// Các hộp thoại của màn Đơn hàng (prototype Fulfillment.jsx) — plan G11.16–G11.17: Từ chối đơn (lý do,
// react-hook-form + zod), Chi tiết đơn (10.3 GET detail), Yêu cầu lấy hàng (so sánh hãng, bàn giao ĐVVC),
// Phiếu gửi hàng (in).
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import logomark from '@/assets/brand/logomark.svg';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Badge,
  Button,
  Icon,
  Modal,
  Select,
  Skeleton,
  StatusBadge,
  Textarea,
  toast,
} from '@/shared/ui';
import {
  useCreateShipment,
  useRejectOrder,
  useSellerOrder,
  useShippingLabel,
  useShippingQuotes,
} from '../application/useFulfillment';
import { CARRIER_NAME, PAYMENT_NAME, REJECT_REASONS, pickQuote } from '../domain/fulfillment.rules';
import { rejectSchema, type RejectValues } from '../domain/fulfillment.schema';
import { orderBadge } from '../domain/order.rules';
import type { SellerOrder } from '../domain/order.types';
import { labelCls, Row, When } from './OrderParts';

export function RejectModal({
  order,
  onClose,
}: {
  order: SellerOrder | null;
  onClose: () => void;
}) {
  const { t } = useT();
  const reject = useRejectOrder();
  const form = useForm<RejectValues>({
    resolver: zodResolver(rejectSchema),
    defaultValues: { reason: 'OUT_OF_STOCK', note: '' },
  });
  useEffect(() => {
    if (order) form.reset({ reason: 'OUT_OF_STOCK', note: '' });
  }, [order, form]);
  const reason = form.watch('reason');
  const note = form.watch('note');
  const err = form.formState.errors.note?.message;
  const submit = form.handleSubmit((v) => {
    if (!order) return;
    reject.mutate(
      {
        id: order.id,
        reason: v.reason,
        note: v.note || null,
        label: t(`seller-orders.reject.reasons.${v.reason}`),
      },
      {
        onSuccess: () => {
          toast.success(
            t(
              order.payment === 'COD'
                ? 'seller-orders.reject.done_cod'
                : 'seller-orders.reject.done',
            ),
          );
          onClose();
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  });
  return (
    <Modal
      open={!!order}
      onClose={onClose}
      width={460}
      title={order ? t('seller-orders.reject.title', { code: order.code }) : ''}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant="danger"
            loading={reject.isPending}
            disabled={reason === 'OTHER' && !note.trim()}
            onClick={() => void submit()}
          >
            {t('seller-orders.reject.submit')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-3" onSubmit={(e) => void submit(e)} noValidate>
        <Select
          label={t('seller-orders.reject.reason')}
          value={reason}
          onChange={(v) => form.setValue('reason', v, { shouldValidate: true })}
          options={REJECT_REASONS.map((r) => ({
            value: r,
            label: t(`seller-orders.reject.reasons.${r}`),
          }))}
        />
        {reason === 'OTHER' && (
          <Textarea
            aria-label={t('seller-orders.reject.note')}
            placeholder={t('seller-orders.reject.note_placeholder')}
            rows={3}
            maxLength={500}
            error={err ? t(err) : undefined}
            {...form.register('note')}
          />
        )}
        <p className="m-0 text-xs text-muted leading-normal">{t('seller-orders.reject.hint')}</p>
      </form>
    </Modal>
  );
}

export function DetailModal({ id, onClose }: { id: number | null; onClose: () => void }) {
  const { t, locale } = useT();
  const q = useSellerOrder(id);
  const o = q.data;
  const b = o ? orderBadge(o) : null;
  return (
    <Modal
      open={id != null}
      onClose={onClose}
      width={520}
      title={
        o ? t('seller-orders.detail.title', { code: o.code }) : t('seller-orders.detail.loading')
      }
      footer={<Button onClick={onClose}>{t('common.action.close')}</Button>}
    >
      {!o || !b ? (
        <Skeleton lines={6} />
      ) : (
        <div className="flex flex-col gap-3.5">
          <div className="flex gap-2 items-center flex-wrap">
            <StatusBadge status={b.tone} label={t(b.key)} />
            <Badge variant="neutral">{PAYMENT_NAME[o.payment]}</Badge>
            <span className="ml-auto text-xs text-muted">
              {t('seller-orders.detail.placed')} <When iso={o.createdAt} />
            </span>
          </div>
          <div>
            <div className={labelCls}>{t('seller-orders.detail.recipient')}</div>
            <div className="text-sm text-body">
              {o.address.recipient} · {o.address.phone}
              <br />
              {o.address.full}
            </div>
          </div>
          <div>
            <div className={labelCls}>{t('seller-orders.detail.items')}</div>
            {o.items.map((i, k) => (
              <Row
                key={k}
                k={`${i.name} · ${i.variant} × ${i.quantity}`}
                v={formatVND(i.unitPrice * i.quantity, { locale })}
              />
            ))}
            <div className="border-0 border-t border-dashed border-line-strong mt-1.5 pt-1.5">
              <Row k={t('seller-orders.detail.paid')} v={formatVND(o.total, { locale })} strong />
            </div>
          </div>
          {o.note && (
            <div>
              <div className={labelCls}>{t('seller-orders.card.note')}</div>
              <div className="text-sm text-body">{o.note}</div>
            </div>
          )}
          {o.shipment?.trackingCode && (
            <div>
              <div className={labelCls}>{t('seller-orders.detail.shipping')}</div>
              <Row
                k={CARRIER_NAME[o.shipment.carrier]}
                v={<span className="code">{o.shipment.trackingCode}</span>}
              />
              <Row
                k={t('seller-orders.detail.carrier_status')}
                v={t(`seller-orders.shipment.${o.shipment.status}`)}
              />
            </div>
          )}
          {o.rejectReason && (
            <div className="text-xs text-red-600">
              {t('seller-orders.card.rejected', { reason: o.rejectReason })}
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}

export function RouteModal({
  order,
  onClose,
  onDone,
}: {
  order: SellerOrder | null;
  onClose: () => void;
  onDone: (id: number) => void;
}) {
  const { t, locale } = useT();
  const quotes = useShippingQuotes(order?.id ?? null);
  const ship = useCreateShipment();
  const best = quotes.data ? pickQuote(quotes.data) : null;
  const busy = ship.isPending;
  return (
    <Modal
      open={!!order}
      onClose={() => !busy && onClose()}
      dismissible={!busy}
      width={460}
      title={t('seller-orders.route.title')}
      footer={
        <>
          <Button variant="ghost" disabled={busy} onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            iconLeft="truck"
            loading={busy}
            disabled={!best}
            onClick={() =>
              order &&
              ship.mutate(order.id, {
                onSuccess: () => {
                  toast.success(t('seller-orders.route.done'));
                  onDone(order.id);
                },
                onError: (e) => toast.error(errorMessage(e, t)),
              })
            }
          >
            {busy ? t('seller-orders.route.placing') : t('seller-orders.route.submit')}
          </Button>
        </>
      }
    >
      {order && (
        <div>
          <p className="m-0 mb-3 text-base text-body">
            {t('seller-orders.route.intro_a')} <b className="code">{order.code}</b>
            {t('seller-orders.route.intro_b')}
          </p>
          {!quotes.data ? (
            <Skeleton lines={2} />
          ) : (
            quotes.data.map((c) => {
              const on = c === best;
              return (
                <div
                  key={c.carrier}
                  className={`flex items-center gap-2.5 px-3 py-2.5 mb-2 rounded-md border-[1.5px] border-solid ${
                    on ? 'border-primary bg-primary-tint' : 'border-line-subtle bg-surface-card'
                  }`}
                >
                  <Icon name="truck" size={17} className="text-muted" />
                  <b className="flex-1 font-semibold text-sm text-strong">
                    {CARRIER_NAME[c.carrier]}
                  </b>
                  <span className="text-xs text-muted">
                    {t('seller-orders.route.days', { count: c.estimatedDays })}
                  </span>
                  <span className="font-semibold text-sm">{formatVND(c.fee, { locale })}</span>
                  {on && <Badge variant="primary">{t('seller-orders.route.selected')}</Badge>}
                </div>
              );
            })
          )}
          <p className="m-0 text-xs text-muted">{t('seller-orders.route.settings_hint')}</p>
        </div>
      )}
    </Modal>
  );
}

const BARS = Array.from({ length: 48 }, (_, i) => ((i * 7) % 3) + 1);

export function WaybillModal({ id, onClose }: { id: number | null; onClose: () => void }) {
  const { t, locale } = useT();
  const q = useShippingLabel(id);
  const l = q.data;
  return (
    <Modal
      open={id != null}
      onClose={onClose}
      width={420}
      title={t('seller-orders.waybill.title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.close')}
          </Button>
          <Button iconLeft="printer" disabled={!l} onClick={() => window.print()}>
            {t('seller-orders.waybill.print')}
          </Button>
        </>
      }
    >
      {!l ? (
        <Skeleton lines={6} />
      ) : (
        <div className="border-2 border-dashed border-line-strong rounded-md p-[18px]">
          <div className="flex items-center gap-2 pb-3 border-0 border-b border-solid border-line">
            <img src={logomark.src} width={28} height={28} alt="" />
            <div className="flex flex-col">
              <span className="font-extrabold text-base text-strong">
                {CARRIER_NAME[l.carrier]}
              </span>
              <span className="text-[11px] text-subtle">{t('seller-orders.waybill.partner')}</span>
            </div>
            <Badge variant="mint" icon="shield-check" className="ml-auto">
              {l.prepaid ? t('seller-orders.waybill.prepaid') : t('seller-orders.waybill.cod')}
            </Badge>
          </div>
          <div className="flex gap-[1.5px] h-[54px] mt-3.5 mb-1.5 items-stretch" aria-hidden="true">
            {BARS.map((f, i) => (
              <span key={i} style={{ flex: f }} className={i % 2 ? 'bg-white' : 'bg-ink-900'} />
            ))}
          </div>
          <div className="code text-center font-bold text-base tracking-[.12em] text-strong mb-3.5">
            {l.trackingCode}
          </div>
          <Row k={t('seller-orders.waybill.recipient')} v={l.recipient} />
          <Row k={t('seller-orders.waybill.address')} v={l.shipTo} />
          <Row
            k={t('seller-orders.waybill.parcel')}
            v={t('seller-orders.waybill.items', { count: l.itemCount })}
          />
          <Row
            k={t('seller-orders.waybill.collect')}
            v={
              l.prepaid
                ? t('seller-orders.waybill.collect_none')
                : formatVND(l.codAmount, { locale })
            }
            strong={!l.prepaid}
          />
        </div>
      )}
    </Modal>
  );
}

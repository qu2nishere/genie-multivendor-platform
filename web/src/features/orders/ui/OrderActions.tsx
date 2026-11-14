// Nút trên đơn theo allowed_actions (prototype Account.jsx Actions) + các hộp thoại đi kèm — plan
// G10.32–G10.34, G10.37: Thanh toán ngay (6.12 qua PaymentFlow của checkout), Hủy đơn có lý do,
// Đã nhận hàng (Genie chuyển tiền cho shop), Mua lại (6.6), Liên hệ shop, Theo dõi, Đánh giá, Trả hàng.
import { useState, type ReactNode } from 'react';
import { buyAgain } from '@/features/cart';
import { PaymentFlow, useCheckoutSession } from '@/features/checkout';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { Button, ConfirmDialog, Select, Textarea, toast } from '@/shared/ui';
import { useCancelOrders, useConfirmReceipt } from '../application/useOrders';
import { CANCEL_REASONS } from '../domain/order.rules';
import type { CancelReason, OrderItem, OrderSummary } from '../domain/order.types';

export const orderHref = (id: number) => `/orders/${id}`;
export const chatHref = (o: Pick<OrderSummary, 'id' | 'shop'>) =>
  `/chat?shop=${o.shop.id}&order=${o.id}`;

/** Hành động cần hộp thoại — một "host" cho cả trang, thẻ đơn chỉ gọi. */
export interface OrderHandlers {
  pay: (sessionId: number) => void;
  cancel: (orders: OrderSummary[]) => void;
  confirm: (o: OrderSummary) => void;
  again: (items: OrderItem[]) => void;
}

export function useOrderHandlers(): { handlers: OrderHandlers; dialogs: ReactNode } {
  const { t, locale } = useT();
  const [paySession, setPaySession] = useState<number | null>(null);
  const session = useCheckoutSession(paySession);
  const [cancelling, setCancelling] = useState<OrderSummary[] | null>(null);
  const [reason, setReason] = useState<CancelReason>('CHANGE_OF_MIND');
  const [note, setNote] = useState('');
  const [confirming, setConfirming] = useState<OrderSummary | null>(null);
  const cancel = useCancelOrders();
  const confirm = useConfirmReceipt();

  const handlers: OrderHandlers = {
    pay: setPaySession,
    cancel: (orders) => {
      setReason('CHANGE_OF_MIND');
      setNote('');
      setCancelling(orders);
    },
    confirm: setConfirming,
    again: (items) =>
      void buyAgain(items.map((i) => ({ variantId: i.variantId, qty: i.qty })))
        .then((r) => {
          if (r.added === 0) {
            toast.error(t('orders.again.none'));
            return;
          }
          toast.success(t(r.skipped ? 'orders.again.partial' : 'orders.again.added'));
          window.location.assign('/cart');
        })
        .catch((e) => toast.error(errorMessage(e, t))),
  };

  const paidOnline = (cancelling ?? []).some(
    (o) => o.status !== 'PENDING_PAYMENT' && o.paymentMethod !== 'COD',
  );
  const dialogs = (
    <>
      {session.data && (
        <PaymentFlow session={session.data} pick onClose={() => setPaySession(null)} />
      )}
      <ConfirmDialog
        open={cancelling !== null}
        tone="danger"
        title={t('orders.cancel.title')}
        confirmLabel={t('orders.cancel.confirm')}
        cancelLabel={t('orders.cancel.keep')}
        loading={cancel.isPending}
        onCancel={() => setCancelling(null)}
        onConfirm={() => {
          if (!cancelling) return;
          if (reason === 'OTHER' && !note.trim()) {
            toast.error(t('orders.cancel.note_required'));
            return;
          }
          cancel.mutate(
            { ids: cancelling.map((o) => o.id), reason, note: note.trim() || null },
            {
              onSuccess: (r) => {
                toast.success(
                  t(cancelling.length > 1 ? 'orders.cancel.done_group' : 'orders.cancel.done') +
                    (r.refund ? t('orders.cancel.refund') : ''),
                );
                setCancelling(null);
              },
              onError: (e) => toast.error(errorMessage(e, t)),
            },
          );
        }}
      >
        <p className="m-0 mb-3 text-base text-body leading-normal">
          {t(
            cancelling && cancelling.length > 1 ? 'orders.cancel.text_group' : 'orders.cancel.text',
            { count: cancelling?.length ?? 1 },
          )}
          {paidOnline && ` ${t('orders.cancel.refund_note')}`}
        </p>
        <Select
          label={t('orders.cancel.reason')}
          value={reason}
          onChange={(v) => setReason(v as CancelReason)}
          options={CANCEL_REASONS.map((r) => ({
            value: r,
            label: t(`orders.cancel.reasons.${r}`),
          }))}
        />
        {reason === 'OTHER' && (
          <Textarea
            className="mt-3"
            label={t('orders.cancel.note')}
            value={note}
            maxLength={500}
            onChange={(e) => setNote(e.target.value)}
          />
        )}
      </ConfirmDialog>
      <ConfirmDialog
        open={confirming !== null}
        title={t('orders.receipt.title')}
        message={t('orders.receipt.text', {
          amount: formatVND(confirming?.grandTotal ?? 0, { locale }),
          shop: confirming?.shop.name ?? '',
        })}
        confirmLabel={t('orders.receipt.confirm')}
        cancelLabel={t('common.action.cancel')}
        loading={confirm.isPending}
        onCancel={() => setConfirming(null)}
        onConfirm={() =>
          confirming &&
          confirm.mutate(confirming.id, {
            onSuccess: () => {
              toast.success(t('orders.receipt.done'));
              setConfirming(null);
            },
            onError: (e) => toast.error(errorMessage(e, t)),
          })
        }
      />
    </>
  );
  return { handlers, dialogs };
}

/** Nút trên thẻ đơn (thứ tự và kiểu bám prototype Actions). */
export function OrderActionButtons({ o, h }: { o: OrderSummary; h: OrderHandlers }) {
  const { t } = useT();
  const a = new Set(o.actions);
  const track = (primary: boolean) => (
    <Button
      size="sm"
      variant={primary ? 'primary' : 'outline'}
      iconLeft="truck"
      href={orderHref(o.id)}
    >
      {t('orders.action.track')}
    </Button>
  );
  const chat = a.has('CHAT') && (
    <Button size="sm" variant="ghost" iconLeft="message-circle" href={chatHref(o)}>
      {t('orders.action.chat')}
    </Button>
  );
  const again = a.has('BUY_AGAIN') && (
    <Button size="sm" variant="outline" onClick={() => h.again(o.items)}>
      {t('orders.action.again')}
    </Button>
  );
  const cancel = a.has('CANCEL') && (
    <Button size="sm" variant="ghost" className="text-red-600" onClick={() => h.cancel([o])}>
      {t('orders.action.cancel')}
    </Button>
  );

  if (o.status === 'PENDING_PAYMENT')
    return (
      <>
        {a.has('PAY') && o.sessionId != null ? (
          <Button size="sm" onClick={() => h.pay(o.sessionId as number)}>
            {t('orders.action.pay')}
          </Button>
        ) : (
          <Button size="sm" variant="outline" iconLeft="clock" disabled>
            {t('orders.action.await_shop')}
          </Button>
        )}
        {cancel}
      </>
    );
  if (a.has('CONFIRM_RECEIPT') || (a.has('REQUEST_RETURN') && o.status !== 'COMPLETED'))
    return (
      <>
        {a.has('CONFIRM_RECEIPT') && (
          <Button size="sm" iconLeft="check" onClick={() => h.confirm(o)}>
            {t('orders.action.received')}
          </Button>
        )}
        {a.has('REQUEST_RETURN') && (
          <Button
            size="sm"
            variant="outline"
            iconLeft="undo-2"
            href={`${orderHref(o.id)}?return=1`}
          >
            {t('orders.action.return')}
          </Button>
        )}
        {track(false)}
      </>
    );
  if (a.has('VIEW_RETURN'))
    return (
      <>
        <Button size="sm" iconLeft="undo-2" href={`${orderHref(o.id)}#return`}>
          {t('orders.action.view_return')}
        </Button>
        {chat}
      </>
    );
  if (o.status === 'COMPLETED')
    return (
      <>
        {a.has('REVIEW') ? (
          <Button size="sm" iconLeft="star" href={`${orderHref(o.id)}?review=1`}>
            {t('orders.action.review')}
          </Button>
        ) : (
          <Button size="sm" iconLeft="star" disabled>
            {t('orders.action.reviewed')}
          </Button>
        )}
        {again}
        {chat}
      </>
    );
  if (a.has('TRACK'))
    return (
      <>
        {track(!cancel)}
        {chat}
        {cancel}
      </>
    );
  return (
    <>
      {again}
      {chat}
    </>
  );
}

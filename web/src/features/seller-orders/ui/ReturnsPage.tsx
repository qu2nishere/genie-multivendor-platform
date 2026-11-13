// Trả hàng phía shop (prototype Fulfillment.jsx Returns) — plan G11.18: shop có 48 giờ phản hồi (quá hạn hệ
// thống tự đồng ý theo phương án khách chọn); xem bằng chứng; Yêu cầu bổ sung (1 lần) / Từ chối / Đồng ý nhận
// lại hàng / Đồng ý hoàn tiền / Đã nhận hàng hoàn / Nhờ Sàn phân xử.
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { Badge, Button, EmptyState, Icon, Modal, Skeleton, Textarea, toast } from '@/shared/ui';
import { useRefundAction, useSellerRefunds } from '../application/useFulfillment';
import { sellerNow } from '../application/useSellerOrders';
import { needsText, REFUND_TONE, refundActions, refundHoursLeft } from '../domain/refund.rules';
import type { SellerRefund, SellerRefundAction } from '../domain/refund.types';
import { labelCls, OrderTabs } from './OrderParts';

const BTN: Record<
  SellerRefundAction,
  {
    variant: 'ghost' | 'outline' | 'secondary' | 'primary';
    icon?: 'hand-coins' | 'check';
    danger?: boolean;
  }
> = {
  REQUEST_EVIDENCE: { variant: 'ghost' },
  REJECT: { variant: 'outline', danger: true },
  APPROVE_WITH_RETURN: { variant: 'secondary' },
  APPROVE_REFUND_ONLY: { variant: 'primary', icon: 'hand-coins' },
  CONFIRM_RETURN_RECEIVED: { variant: 'primary', icon: 'check' },
  ESCALATE: { variant: 'outline' },
};

function Due({ r }: { r: SellerRefund }) {
  const { t } = useT();
  const h = refundHoursLeft(r.deadlineAt, sellerNow());
  const text =
    h !== null
      ? t('seller-orders.returns.due_hours', { hours: h })
      : r.status === 'AWAITING_RETURN_SHIPMENT'
        ? t('seller-orders.returns.due_return')
        : null;
  if (!text) return null;
  return (
    <span className="ml-auto inline-flex items-center gap-[5px] text-xs text-amber-600">
      <Icon name="clock" size={13} />
      {text}
    </span>
  );
}

function RefundCard({ r, onAct }: { r: SellerRefund; onAct: (a: SellerRefundAction) => void }) {
  const { t, locale } = useT();
  const actions = refundActions(r);
  return (
    <article className="bg-surface-card rounded-lg shadow-sm overflow-hidden">
      <div className="flex items-center gap-2.5 px-[18px] py-3 bg-gray-25 border-0 border-b border-solid border-line-subtle flex-wrap">
        <span className="code text-[13px] font-semibold text-strong">{r.code}</span>
        <span className="code text-xs text-muted">{r.order.code}</span>
        <Badge variant={REFUND_TONE[r.status]}>
          {t(`seller-orders.returns.status.${r.status}`)}
        </Badge>
        <Due r={r} />
      </div>
      <div className="grid grid-cols-[minmax(0,1fr)_auto] gap-[18px] px-[18px] py-3.5 max-sm:grid-cols-1">
        <div className="text-sm text-body leading-[1.7]">
          <div>
            <b>{r.customerName}</b> · {r.items.map((i) => `${i.name} · ${i.variant}`).join(', ')}
          </div>
          <div>
            {t('seller-orders.returns.plan')}: <b>{t(`seller-orders.returns.type.${r.type}`)}</b>
          </div>
          <div>
            {t('seller-orders.returns.reason')}: {t(`refunds.reason.${r.reasonCode}`)}
          </div>
          <div className="flex items-center gap-2 mt-2 flex-wrap">
            {r.evidence
              .filter((e) => e.kind === 'IMAGE')
              .map((e) => (
                <a key={e.url} href={e.url} target="_blank" rel="noopener">
                  <img
                    src={e.url}
                    alt={t('seller-orders.returns.evidence')}
                    width={52}
                    height={52}
                    className="w-[52px] h-[52px] rounded-sm object-cover border border-solid border-line-subtle"
                  />
                </a>
              ))}
            {r.evidence
              .filter((e) => e.kind === 'VIDEO')
              .map((e) => (
                <span
                  key={e.name}
                  className="inline-flex items-center gap-[5px] text-xs text-muted"
                >
                  <Icon name="file-text" size={14} />
                  {e.name}
                </span>
              ))}
          </div>
        </div>
        <div className="text-right max-sm:text-left">
          <div className="text-xs text-muted">{t('seller-orders.returns.amount')}</div>
          <div className="font-extrabold text-xl text-price tabular-nums">
            {formatVND(r.amount, { locale })}
          </div>
        </div>
      </div>
      {actions.length > 0 && (
        <div className="flex gap-2 justify-end px-[18px] pb-3.5 flex-wrap">
          {actions.map((a) => (
            <Button
              key={a}
              size="sm"
              variant={BTN[a].variant}
              iconLeft={BTN[a].icon}
              className={BTN[a].danger ? '!text-red-600' : undefined}
              onClick={() => onAct(a)}
            >
              {t(`seller-orders.returns.act.${a}.btn`)}
            </Button>
          ))}
        </div>
      )}
    </article>
  );
}

export default function ReturnsPage() {
  const { t } = useT();
  const q = useSellerRefunds();
  const act = useRefundAction();
  const [cur, setCur] = useState<{ r: SellerRefund; a: SellerRefundAction } | null>(null);
  const [text, setText] = useState('');
  const ask = cur ? needsText(cur.a) : false;
  const done = () => {
    if (!cur) return;
    act.mutate(
      { id: cur.r.id, action: cur.a, text: ask ? text.trim() : undefined },
      {
        onSuccess: () => {
          toast.success(t(`seller-orders.returns.act.${cur.a}.done`));
          setCur(null);
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  };
  return (
    <div className="max-w-[1180px]">
      <OrderTabs value="RETURN" />
      <div className="mt-4 flex flex-col gap-3.5">
        <div className="flex gap-2 items-center bg-blue-50 rounded-md px-3.5 py-2.5 text-sm text-body">
          <Icon name="info" size={16} className="text-blue-600 flex-none" />
          {t('seller-orders.returns.notice')}
        </div>
        {!q.data ? (
          <Skeleton lines={8} />
        ) : q.data.length === 0 ? (
          <div className="bg-surface-card rounded-lg py-14">
            <EmptyState icon="refresh-cw" text={t('seller-orders.returns.empty')} />
          </div>
        ) : (
          q.data.map((r) => (
            <RefundCard
              key={r.id}
              r={r}
              onAct={(a) => {
                setText('');
                setCur({ r, a });
              }}
            />
          ))
        )}
      </div>
      <Modal
        open={!!cur}
        onClose={() => !act.isPending && setCur(null)}
        width={460}
        title={cur ? t(`seller-orders.returns.act.${cur.a}.title`) : ''}
        footer={
          cur && (
            <>
              <Button variant="ghost" disabled={act.isPending} onClick={() => setCur(null)}>
                {t('common.action.cancel')}
              </Button>
              <Button
                variant={cur.a === 'REJECT' ? 'danger' : 'primary'}
                loading={act.isPending}
                disabled={ask && !text.trim()}
                onClick={done}
              >
                {t(`seller-orders.returns.act.${cur.a}.btn`)}
              </Button>
            </>
          )
        }
      >
        {cur && (
          <div>
            <p className="m-0 mb-3 text-sm text-muted leading-normal">
              {t(`seller-orders.returns.act.${cur.a}.note`)}
            </p>
            {ask && (
              <>
                <label htmlFor="refund-text" className={labelCls}>
                  {t(`seller-orders.returns.act.${cur.a}.ask`)}
                </label>
                <Textarea
                  id="refund-text"
                  rows={3}
                  maxLength={500}
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                />
              </>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

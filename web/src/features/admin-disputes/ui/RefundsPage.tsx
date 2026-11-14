// Trả hàng & Hoàn tiền — phân xử (prototype admin/Disputes.jsx Refunds) — plan G12.15: đếm Đang mở /
// Đã hoàn / Từ chối; thẻ tranh chấp (mã yêu cầu, mã đơn, hai bên, lý do, bằng chứng hai bên, số tiền);
// Phân xử (4 phán quyết + biên bản), phong tỏa / gỡ phong tỏa ký quỹ, "Hàng đã về kho shop", "Mở lại";
// khối "Trạng thái hoàn tiền Gateway" (đồng bộ, thử lại, đã chuyển khoản tay nội dung TRRF…).
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime, formatVND } from '@/shared/lib/format';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Avatar,
  Badge,
  Button,
  EmptyState,
  Icon,
  Modal,
  Skeleton,
  StatusBadge,
  Textarea,
  toast,
} from '@/shared/ui';
import { useDisputeCommand, useDisputes, type DisputeCommand } from '../application/useDisputes';
import { COUNTED, disputeActions, OUTCOMES, refundActions } from '../domain/dispute.rules';
import {
  freezeSchema,
  resolveSchema,
  type FreezeValues,
  type ResolveValues,
} from '../domain/dispute.schema';
import type { Dispute, GatewayRefund, Media } from '../domain/dispute.types';

const info = 'text-xs text-muted';
const val = 'text-sm font-medium text-body';

function useCommand() {
  const { t } = useT();
  const cmd = useDisputeCommand();
  const exec = (c: DisputeCommand, done: string, after?: () => void) =>
    cmd.mutate(c, {
      onSuccess: () => {
        toast.success(done);
        after?.();
      },
      onError: (e) => toast.error(errorMessage(e, t)),
    });
  return { exec, pending: cmd.isPending };
}

function Evidence({ items, empty }: { items: Media[]; empty: string }) {
  const { t } = useT();
  if (items.length === 0) return <span className="text-xs text-subtle">{empty}</span>;
  return (
    <span className="flex gap-1.5 mt-[5px] items-center flex-wrap">
      {items.map((m, i) =>
        m.type === 'IMAGE' ? (
          <a
            key={i}
            href={m.url}
            target="_blank"
            rel="noreferrer"
            className="rounded-sm"
            aria-label={t('admin-disputes.evidence_image', { n: i + 1 })}
          >
            <img
              src={m.url}
              alt=""
              width={44}
              height={44}
              className="w-11 h-11 rounded-sm object-cover"
            />
          </a>
        ) : (
          <a
            key={i}
            href={m.url}
            target="_blank"
            rel="noreferrer"
            className="inline-flex gap-1 items-center text-xs text-muted no-underline hover:underline"
          >
            <Icon name="file-text" size={14} />
            {m.url.split('/').pop()}
          </a>
        ),
      )}
    </span>
  );
}

function Field({ k, v, mono, price }: { k: string; v: string; mono?: boolean; price?: boolean }) {
  return (
    <div>
      <div className={info}>{k}</div>
      <div
        className={cn(
          'mt-[3px] break-all',
          mono ? 'font-mono text-[13px] text-strong' : 'text-sm font-semibold text-strong',
          price && 'text-price font-bold',
        )}
      >
        {v}
      </div>
    </div>
  );
}

function GatewayCard({ r }: { r: GatewayRefund }) {
  const { t, locale } = useT();
  const { exec, pending } = useCommand();
  const manual = r.manualTransfer !== null;
  const acts = refundActions(r);
  const tone = r.status === 'SUCCESS' ? 'COMPLETED' : r.status === 'FAILED' ? 'ERROR' : 'PENDING';
  const label =
    manual && r.status === 'PENDING'
      ? t('admin-disputes.gateway.manual_pending')
      : t(`admin-disputes.gateway.status.${r.status}`);
  return (
    <section
      className="mt-3.5 border border-solid border-line-subtle rounded-md overflow-hidden"
      aria-label={t('admin-disputes.gateway.title')}
    >
      <div className="flex items-center gap-2 px-3.5 py-[11px] bg-gray-25 border-0 border-b border-solid border-line-subtle">
        <Icon name="credit-card" size={16} className="text-primary" />
        <h3 className="m-0 text-sm font-bold text-strong">{t('admin-disputes.gateway.title')}</h3>
        <span className="ml-auto">
          <StatusBadge status={tone} label={label} />
        </span>
      </div>
      <div className="grid grid-cols-3 gap-x-[18px] gap-y-3 p-3.5 max-md:grid-cols-2">
        <Field
          k={t('admin-disputes.gateway.gateway')}
          v={r.gateway ?? t('admin-disputes.gateway.bank_transfer')}
        />
        <Field k={t('admin-disputes.gateway.txn')} v={r.originalTransactionId ?? '—'} mono />
        <Field k={t('admin-disputes.gateway.refund_id')} v={r.gatewayRefundId ?? '—'} mono />
        <Field k={t('admin-disputes.gateway.amount')} v={formatVND(r.amount, { locale })} price />
        <Field
          k={t('admin-disputes.gateway.created')}
          v={formatDateTime(r.createdAt, { locale })}
        />
        {r.manualTransfer && (
          <>
            <Field k={t('admin-disputes.gateway.to')} v={r.manualTransfer.toAccount} />
            <Field
              k={t('admin-disputes.gateway.content')}
              v={r.manualTransfer.transferContent}
              mono
            />
          </>
        )}
        <div className="flex items-end justify-end max-md:col-span-2 max-md:justify-start">
          {manual ? (
            <Button
              variant="secondary"
              size="sm"
              iconLeft="check"
              disabled={!acts.includes('mark_transferred')}
              loading={pending}
              onClick={() =>
                exec(
                  { kind: 'mark_transferred', refundId: r.id, bankReference: null },
                  t('admin-disputes.gateway.marked'),
                )
              }
            >
              {t('admin-disputes.gateway.mark')}
            </Button>
          ) : (
            acts.includes('sync') && (
              <Button
                variant="secondary"
                size="sm"
                iconLeft="refresh-cw"
                loading={pending}
                onClick={() =>
                  exec({ kind: 'sync', refundId: r.id }, t('admin-disputes.gateway.synced'))
                }
              >
                {pending ? t('admin-disputes.gateway.syncing') : t('admin-disputes.gateway.sync')}
              </Button>
            )
          )}
        </div>
      </div>
      {r.status === 'FAILED' && (
        <div className="mx-3.5 mb-3.5 flex items-start gap-2.5 bg-red-50 border border-solid border-red-200 rounded-md px-3.5 py-[11px]">
          <Icon name="triangle-alert" size={18} className="text-red-600 flex-none mt-px" />
          <div className="flex-1">
            <div className="text-sm font-semibold text-red-600">
              {t('admin-disputes.gateway.failed')}
            </div>
            <div className="text-xs text-body mt-0.5">
              {t('admin-disputes.gateway.failed_hint')}
            </div>
          </div>
          <Button
            variant="danger"
            size="sm"
            iconLeft="refresh-cw"
            loading={pending}
            onClick={() =>
              exec({ kind: 'retry', refundId: r.id }, t('admin-disputes.gateway.retried'))
            }
          >
            {t('admin-disputes.gateway.retry')}
          </Button>
        </div>
      )}
    </section>
  );
}

function StatusChips({ d }: { d: Dispute }) {
  const { t } = useT();
  return (
    <>
      {d.status === 'OPEN' && (
        <StatusBadge status="PENDING" label={t('admin-disputes.state.OPEN')} />
      )}
      {d.status === 'AWAITING_RETURN' && (
        <Badge variant="primary" icon="truck">
          {t('admin-disputes.state.AWAITING_RETURN')}
        </Badge>
      )}
      {d.status === 'REFUNDED' && (
        <Badge variant="success" icon="circle-check">
          {t('admin-disputes.state.REFUNDED')}
        </Badge>
      )}
      {d.status === 'DENIED' && (
        <Badge variant="danger" icon="ban">
          {t('admin-disputes.state.DENIED')}
        </Badge>
      )}
      {d.escrow.status === 'FROZEN' && (
        <Badge variant="neutral" icon="lock">
          {t('admin-disputes.frozen')}
        </Badge>
      )}
    </>
  );
}

function DisputeCard({
  d,
  onResolve,
  onFreeze,
}: {
  d: Dispute;
  onResolve: () => void;
  onFreeze: () => void;
}) {
  const { t, locale } = useT();
  const { exec, pending } = useCommand();
  const acts = disputeActions(d);
  const imgs = d.customerEvidence.filter((m) => m.type === 'IMAGE').length;
  const vids = d.customerEvidence.length - imgs;
  return (
    <article
      className="bg-surface-card rounded-lg shadow-sm p-[18px] mb-3.5"
      aria-labelledby={`dispute-${d.id}`}
    >
      <div className="flex items-center gap-2.5 mb-3.5 flex-wrap">
        <h2 id={`dispute-${d.id}`} className="m-0 font-mono text-[13px] font-semibold text-strong">
          {d.code}
        </h2>
        <span className="font-mono text-xs text-muted bg-gray-100 px-[7px] py-0.5 rounded-sm">
          {d.order.code}
        </span>
        <StatusChips d={d} />
        <span className="ml-auto text-xs text-subtle">
          {t('admin-disputes.opened', { at: formatDateTime(d.openedAt, { locale }) })}
        </span>
        {acts.includes('freeze') && (
          <Button variant="ghost" size="sm" iconLeft="lock" onClick={onFreeze}>
            {t('admin-disputes.freeze.action')}
          </Button>
        )}
        {acts.includes('unfreeze') && (
          <Button
            variant="ghost"
            size="sm"
            iconLeft="lock"
            loading={pending}
            onClick={() =>
              exec(
                { kind: 'unfreeze', id: d.id, escrowId: d.escrow.id },
                t('admin-disputes.freeze.removed'),
              )
            }
          >
            {t('admin-disputes.freeze.remove')}
          </Button>
        )}
      </div>
      <div className="grid grid-cols-[1fr_1fr_auto] gap-[18px] items-center max-md:grid-cols-1">
        <div>
          <div className={info}>{t('admin-disputes.buyer')}</div>
          <div className="flex items-center gap-2 mt-[5px]">
            <Avatar name={d.customer.name} size={28} />
            <span className={val}>{d.customer.name}</span>
          </div>
          <div className={cn(info, 'mt-2.5')}>{t('admin-disputes.reason')}</div>
          <div className={cn(val, 'mt-[3px]')}>{t(`refunds.reason.${d.reasonCode}`)}</div>
        </div>
        <div>
          <div className={info}>{t('admin-disputes.seller')}</div>
          <div className="flex items-center gap-2 mt-[5px]">
            <Avatar name={d.shop.name} size={28} square />
            <span className={val}>{d.shop.name}</span>
          </div>
          <div className={cn(info, 'mt-2.5')}>
            {t('admin-disputes.customer_evidence', { images: imgs, videos: vids })}
          </div>
          <Evidence items={d.customerEvidence} empty="" />
          <div className={cn(info, 'mt-2.5')}>{t('admin-disputes.shop_evidence')}</div>
          <Evidence items={d.shopEvidence} empty={t('admin-disputes.no_shop_evidence')} />
        </div>
        <div className="text-right max-md:text-left">
          <div className={info}>{t('admin-disputes.amount')}</div>
          <div className="text-[22px] font-extrabold text-price tabular-nums mt-1 mb-3">
            {formatVND(d.amount, { locale })}
          </div>
          {acts.includes('resolve') && (
            <Button size="sm" iconLeft="scale" onClick={onResolve}>
              {t('admin-disputes.resolve.action')}
            </Button>
          )}
          {acts.includes('confirm_return') && (
            <Button
              size="sm"
              iconLeft="check"
              loading={pending}
              onClick={() =>
                exec({ kind: 'confirm_return', id: d.id }, t('admin-disputes.return_done'))
              }
            >
              {t('admin-disputes.confirm_return')}
            </Button>
          )}
          {acts.includes('reopen') && (
            <Button
              variant="ghost"
              size="sm"
              loading={pending}
              onClick={() => exec({ kind: 'reopen', id: d.id }, t('admin-disputes.reopened'))}
            >
              {t('admin-disputes.reopen')}
            </Button>
          )}
        </div>
      </div>
      {d.refund && <GatewayCard r={d.refund} />}
    </article>
  );
}

function ResolveModal({ d, onClose }: { d: Dispute | null; onClose: () => void }) {
  const { t, locale } = useT();
  const { exec, pending } = useCommand();
  const form = useForm<ResolveValues>({
    resolver: zodResolver(resolveSchema),
    defaultValues: { outcome: 'REFUND_NOW', verdict: '' },
  });
  useEffect(() => {
    if (d) form.reset({ outcome: 'REFUND_NOW', verdict: '' });
  }, [d, form]);
  const outcome = form.watch('outcome');
  const verdict = form.watch('verdict');
  const err = form.formState.errors.verdict?.message;
  const submit = form.handleSubmit((v) => {
    if (!d) return;
    exec(
      { kind: 'resolve', id: d.id, outcome: v.outcome, verdict: v.verdict },
      t(`admin-disputes.resolve.done.${v.outcome}`),
      onClose,
    );
  });
  return (
    <Modal
      open={!!d}
      onClose={onClose}
      width={540}
      dismissible={!verdict}
      title={d ? t('admin-disputes.resolve.title', { code: d.code }) : ''}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant={outcome === 'DENY' ? 'danger' : 'primary'}
            disabled={!verdict.trim()}
            loading={pending}
            onClick={() => void submit()}
          >
            {t('admin-disputes.resolve.submit')}
          </Button>
        </>
      }
    >
      <form onSubmit={(e) => void submit(e)} noValidate className="flex flex-col gap-2">
        <fieldset className="border-0 p-0 m-0 flex flex-col gap-2">
          <legend className="sr-only">{t('admin-disputes.resolve.outcome')}</legend>
          {OUTCOMES.map((o) => {
            const on = outcome === o;
            return (
              <label
                key={o}
                className={cn(
                  'flex gap-[9px] px-3 py-2.5 border-[1.5px] border-solid rounded-md cursor-pointer',
                  on ? 'border-blue-500 bg-blue-50' : 'border-line bg-surface-card',
                )}
              >
                <input
                  type="radio"
                  value={o}
                  checked={on}
                  onChange={() => form.setValue('outcome', o)}
                  className="accent-blue-500 mt-[3px]"
                />
                <span>
                  <span className="block text-sm font-semibold text-strong">
                    {t(`admin-disputes.outcomes.${o}.label`)}
                  </span>
                  <span className="text-xs text-muted">
                    {t(`admin-disputes.outcomes.${o}.note`)}
                  </span>
                </span>
              </label>
            );
          })}
        </fieldset>
        {d && (
          <div className="text-sm text-muted">
            {t('admin-disputes.amount')}:{' '}
            <b className="text-price">{formatVND(d.amount, { locale })}</b>
          </div>
        )}
        <Textarea
          label={t('admin-disputes.resolve.verdict')}
          placeholder={t('admin-disputes.resolve.verdict_placeholder')}
          rows={3}
          required
          maxLength={2000}
          error={err ? t(err) : undefined}
          wrapperClassName="mt-1"
          {...form.register('verdict')}
        />
      </form>
    </Modal>
  );
}

function FreezeModal({ d, onClose }: { d: Dispute | null; onClose: () => void }) {
  const { t, locale } = useT();
  const { exec, pending } = useCommand();
  const form = useForm<FreezeValues>({
    resolver: zodResolver(freezeSchema),
    defaultValues: { reason: '' },
  });
  useEffect(() => {
    if (d) form.reset({ reason: '' });
  }, [d, form]);
  const reason = form.watch('reason');
  const submit = form.handleSubmit((v) => {
    if (!d) return;
    exec(
      { kind: 'freeze', id: d.id, escrowId: d.escrow.id, reason: v.reason },
      t('admin-disputes.freeze.done'),
      onClose,
    );
  });
  return (
    <Modal
      open={!!d}
      onClose={onClose}
      title={d ? t('admin-disputes.freeze.title', { order: d.order.code }) : ''}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant="danger"
            iconLeft="lock"
            disabled={!reason.trim()}
            loading={pending}
            onClick={() => void submit()}
          >
            {t('admin-disputes.freeze.submit')}
          </Button>
        </>
      }
    >
      {d && (
        <form onSubmit={(e) => void submit(e)} noValidate className="flex flex-col gap-2.5">
          <p className="m-0 text-sm text-muted leading-normal">
            {t('admin-disputes.freeze.desc', {
              amount: formatVND(d.amount, { locale }),
              shop: d.shop.name,
            })}
          </p>
          <Textarea
            label={t('admin-disputes.freeze.reason')}
            rows={2}
            required
            maxLength={500}
            {...form.register('reason')}
          />
        </form>
      )}
    </Modal>
  );
}

const COUNT_TONE = { OPEN: 'warning', REFUNDED: 'success', DENIED: 'danger' } as const;

export default function RefundsPage() {
  const { t } = useT();
  const q = useDisputes();
  const [resolving, setResolving] = useState<Dispute | null>(null);
  const [freezing, setFreezing] = useState<Dispute | null>(null);
  if (q.isPending) return <Skeleton lines={10} />;
  if (q.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(q.error, t)}
      />
    );
  const { items, counts } = q.data;
  return (
    <div className="max-w-[1180px]">
      <div className="flex gap-2 mb-4 flex-wrap">
        {COUNTED.map((s) => (
          <Badge key={s} variant={COUNT_TONE[s as keyof typeof COUNT_TONE]}>
            {t(`admin-disputes.count.${s}`, { count: counts[s] })}
          </Badge>
        ))}
      </div>
      {items.length === 0 ? (
        <EmptyState icon="undo-2" title={t('admin-disputes.empty')} />
      ) : (
        items.map((d) => (
          <DisputeCard
            key={d.id}
            d={d}
            onResolve={() => setResolving(d)}
            onFreeze={() => setFreezing(d)}
          />
        ))
      )}
      <ResolveModal d={resolving} onClose={() => setResolving(null)} />
      <FreezeModal d={freezing} onClose={() => setFreezing(null)} />
    </div>
  );
}

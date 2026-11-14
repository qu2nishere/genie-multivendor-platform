// Flash Sale (prototype admin/FlashSale.jsx) — plan G12.34: danh sách chiến dịch (?id=) + trạng thái,
// chi tiết (khung giờ, số SP đã duyệt / tối đa, hạn đăng ký), bảng sản phẩm shop đăng ký (giá 30 ngày,
// giá Flash, % giảm — khóa "Duyệt" khi giảm < 15%), Lên lịch (cần ≥ 1 SP duyệt) / Kết thúc sớm,
// "Tạo chiến dịch" (rhf + zod). Mở / đóng đúng giờ do cron server — bỏ nút "Mô phỏng: đến giờ mở bán" (D5).
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatDateTime, formatVND } from '@/shared/lib/format';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Badge,
  Button,
  EmptyState,
  Icon,
  Input,
  Modal,
  Skeleton,
  toast,
  type BadgeVariant,
} from '@/shared/ui';
import {
  useCampaign,
  useCampaignAction,
  useCampaigns,
  useCreateCampaign,
  useReviewFlashItem,
} from '../application/useMarketing';
import {
  approvedCount,
  campaignAction,
  canReviewItem,
  flashDiscount,
  MIN_FLASH_DISCOUNT,
} from '../domain/marketing.rules';
import { flashSchema, type FlashValues } from '../domain/marketing.schema';
import type { FlashCampaignDetail, FlashStatus } from '../domain/marketing.types';

const ST: Record<FlashStatus, BadgeVariant> = {
  DRAFT: 'neutral',
  SCHEDULED: 'primary',
  ACTIVE: 'success',
  ENDED: 'neutral',
};
const th = 'px-4 py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-4 py-[11px] whitespace-nowrap';
const box =
  'flex-1 min-w-0 h-[42px] border-[1.5px] border-solid rounded-md px-2.5 text-sm bg-surface-card text-body outline-none focus:border-primary';
const lab = 'block text-sm font-semibold text-strong';

function CreateModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (id: number) => void;
}) {
  const { t } = useT();
  const create = useCreateCampaign();
  const form = useForm<FlashValues>({
    resolver: zodResolver(flashSchema),
    defaultValues: {
      name: '',
      startsAt: '',
      endsAt: '',
      maxItemsPerShop: 20,
      registrationDeadline: '',
    },
  });
  const e = form.formState.errors;
  const m = (x?: string) => (x ? t(x) : undefined);
  const submit = form.handleSubmit((v) =>
    create.mutate(v, {
      onSuccess: (id) => {
        toast.success(t('admin-marketing.flash.created'));
        form.reset();
        onCreated(id);
        onClose();
      },
      onError: (err) => toast.error(errorMessage(err, t)),
    }),
  );
  const timeErr = m(e.startsAt?.message ?? e.endsAt?.message);
  return (
    <Modal
      open={open}
      onClose={onClose}
      width={520}
      dismissible={!form.formState.isDirty}
      title={t('admin-marketing.flash.create_title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button iconLeft="check" loading={create.isPending} onClick={() => void submit()}>
            {t('admin-marketing.flash.create')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-3.5" onSubmit={(ev) => void submit(ev)} noValidate>
        <Input
          label={t('admin-marketing.flash.name')}
          placeholder={t('admin-marketing.flash.name_placeholder')}
          maxLength={100}
          error={m(e.name?.message)}
          {...form.register('name')}
        />
        <fieldset className="border-0 p-0 m-0">
          <legend className={lab}>{t('admin-marketing.flash.window')}</legend>
          <div className="flex items-center gap-2 mt-1.5">
            <input
              type="datetime-local"
              aria-label={t('admin-marketing.flash.start')}
              className={cn(box, e.startsAt ? 'border-red-500' : 'border-line')}
              {...form.register('startsAt')}
            />
            <Icon name="arrow-right" size={16} className="text-subtle flex-none" />
            <input
              type="datetime-local"
              aria-label={t('admin-marketing.flash.end')}
              className={cn(box, e.endsAt ? 'border-red-500' : 'border-line')}
              {...form.register('endsAt')}
            />
          </div>
          {timeErr && (
            <p role="alert" className="m-0 mt-1.5 flex items-center gap-1 text-xs text-red-600">
              <Icon name="circle-alert" size={13} />
              {timeErr}
            </p>
          )}
        </fieldset>
        <div className="grid grid-cols-2 gap-3 max-sm:grid-cols-1">
          <Input
            label={t('admin-marketing.flash.max')}
            inputMode="numeric"
            error={m(e.maxItemsPerShop?.message)}
            {...form.register('maxItemsPerShop', { valueAsNumber: true })}
          />
          <div>
            <label className={lab} htmlFor="fs-deadline">
              {t('admin-marketing.flash.deadline')}
            </label>
            <input
              id="fs-deadline"
              type="date"
              className={cn(
                box,
                'w-full mt-1.5',
                e.registrationDeadline ? 'border-red-500' : 'border-line',
              )}
              {...form.register('registrationDeadline')}
            />
            {e.registrationDeadline && (
              <p role="alert" className="m-0 mt-1.5 text-xs text-red-600">
                {m(e.registrationDeadline.message)}
              </p>
            )}
          </div>
        </div>
      </form>
    </Modal>
  );
}

function Detail({ c }: { c: FlashCampaignDetail }) {
  const { t, locale } = useT();
  const act = useCampaignAction();
  const review = useReviewFlashItem();
  const action = campaignAction(c);
  const vnd = (n: number) => formatVND(n, { locale });
  const run = (kind: 'schedule' | 'end') =>
    act.mutate(
      { id: c.id, kind },
      {
        onSuccess: () => toast.success(t(`admin-marketing.flash.done.${kind}`)),
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
      aria-labelledby="fs-detail"
    >
      <div className="flex items-center gap-3 px-5 py-4 border-0 border-b border-solid border-line-subtle flex-wrap">
        <span className="w-10 h-10 rounded-md bg-flash-50 text-flash-600 grid place-items-center flex-none">
          <Icon name="zap" size={21} />
        </span>
        <div className="flex-1 min-w-[200px]">
          <h2 id="fs-detail" className="m-0 text-lg font-bold text-strong">
            {c.name}
          </h2>
          <div className="text-xs text-muted">
            {formatDateTime(c.startsAt, { locale })} → {formatDateTime(c.endsAt, { locale })} ·{' '}
            {t('admin-marketing.flash.meta', {
              approved: approvedCount(c),
              max: c.maxItemsPerShop,
              deadline: formatDate(c.registrationDeadline, { locale }),
            })}
          </div>
        </div>
        {action?.kind === 'schedule' && (
          <Button
            iconLeft="calendar"
            disabled={!action.enabled}
            title={action.enabled ? undefined : t('admin-marketing.flash.schedule_hint')}
            loading={act.isPending}
            onClick={() => run('schedule')}
          >
            {t('admin-marketing.flash.schedule')}
          </Button>
        )}
        {action?.kind === 'end' && (
          <Button
            variant="danger"
            iconLeft="power"
            loading={act.isPending}
            onClick={() => run('end')}
          >
            {t('admin-marketing.flash.end_early')}
          </Button>
        )}
      </div>
      <h3 className="m-0 px-5 pt-3.5 pb-1.5 text-sm font-bold text-strong">
        {t('admin-marketing.flash.items')}
      </h3>
      {c.items.length === 0 ? (
        <div className="px-5 py-8 text-center text-base text-muted">
          {t('admin-marketing.flash.no_items')}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] border-collapse tabular-nums">
            <thead>
              <tr>
                <th className={th}>{t('admin-marketing.flash.col.product')}</th>
                <th className={th}>{t('admin-marketing.flash.col.shop')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-marketing.flash.col.ref')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-marketing.flash.col.flash')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-marketing.flash.col.off')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-marketing.flash.col.stock')}</th>
                <th
                  className={cn(
                    th,
                    'text-right sticky right-0 shadow-[-6px_0_8px_-6px_rgba(0,0,0,.12)]',
                  )}
                >
                  {t('admin-marketing.flash.col.review')}
                </th>
              </tr>
            </thead>
            <tbody>
              {c.items.map((i) => {
                const off = flashDiscount(i);
                const low = !i.meetsDiscountRule;
                return (
                  <tr
                    key={i.id}
                    className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                  >
                    <td className={td}>{i.productName}</td>
                    <td className={td}>{i.shopName}</td>
                    <td className={cn(td, 'text-right')}>{vnd(i.referencePrice)}</td>
                    <td className={cn(td, 'text-right font-semibold text-price')}>
                      {vnd(i.flashPrice)}
                    </td>
                    <td
                      className={cn(
                        td,
                        'text-right font-semibold',
                        low ? 'text-red-600' : 'text-mint-600',
                      )}
                      title={
                        low
                          ? t('admin-marketing.flash.min_off', { pct: MIN_FLASH_DISCOUNT })
                          : undefined
                      }
                    >
                      −{off}%
                    </td>
                    <td className={cn(td, 'text-right')}>{i.flashStock}</td>
                    <td
                      className={cn(
                        td,
                        'text-right sticky right-0 bg-surface-card shadow-[-6px_0_8px_-6px_rgba(0,0,0,.12)]',
                      )}
                    >
                      {canReviewItem(c, i) ? (
                        <div className="flex gap-1 justify-end">
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() =>
                              review.mutate(
                                { campaignId: c.id, itemId: i.id, status: 'REJECTED' },
                                { onError: (e) => toast.error(errorMessage(e, t)) },
                              )
                            }
                          >
                            {t('admin-marketing.flash.reject')}
                          </Button>
                          <Button
                            size="sm"
                            iconLeft="check"
                            disabled={low}
                            title={
                              low
                                ? t('admin-marketing.flash.min_off', { pct: MIN_FLASH_DISCOUNT })
                                : undefined
                            }
                            onClick={() =>
                              review.mutate(
                                { campaignId: c.id, itemId: i.id, status: 'APPROVED' },
                                { onError: (e) => toast.error(errorMessage(e, t)) },
                              )
                            }
                          >
                            {t('admin-marketing.flash.approve')}
                          </Button>
                        </div>
                      ) : (
                        <Badge
                          variant={
                            i.status === 'APPROVED'
                              ? 'success'
                              : i.status === 'REJECTED'
                                ? 'danger'
                                : 'warning'
                          }
                        >
                          {t(`admin-marketing.flash.item_status.${i.status}`)}
                        </Badge>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default function FlashSalePage() {
  const { t, locale } = useT();
  const list = useCampaigns();
  const [params, setParams] = useSearchParams();
  const [creating, setCreating] = useState(false);
  const rows = list.data ?? [];
  const selId = Number(params.get('id')) || rows[0]?.id || null;
  const detail = useCampaign(selId);
  const select = (id: number) => setParams({ id: String(id) }, { replace: true });
  if (list.isPending) return <Skeleton lines={8} />;
  if (list.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(list.error, t)}
      />
    );
  return (
    <div className="max-w-[1180px] grid grid-cols-[340px_minmax(0,1fr)] gap-[18px] items-start max-lg:grid-cols-1">
      <section
        className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
        aria-labelledby="fs-list"
      >
        <div className="flex items-center px-4 py-3 border-0 border-b border-solid border-line-subtle">
          <h2 id="fs-list" className="m-0 text-sm font-bold text-strong">
            {t('admin-marketing.flash.campaigns')}
          </h2>
          <Button size="sm" iconLeft="plus" className="ml-auto" onClick={() => setCreating(true)}>
            {t('admin-marketing.flash.create_short')}
          </Button>
        </div>
        <ul className="list-none m-0 p-0">
          {rows.map((c) => {
            const on = c.id === selId;
            return (
              <li key={c.id}>
                <button
                  type="button"
                  aria-current={on ? 'true' : undefined}
                  onClick={() => select(c.id)}
                  className={cn(
                    'block w-full text-left border-0 border-b border-solid border-line-subtle px-4 py-3 cursor-pointer focus-visible:outline-none focus-visible:shadow-focus',
                    on ? 'bg-blue-50' : 'bg-surface-card hover:bg-gray-50',
                  )}
                >
                  <span className="flex items-center gap-2">
                    <span className="flex-1 text-sm font-semibold text-strong">{c.name}</span>
                    <Badge variant={ST[c.status]}>
                      {t(`admin-marketing.flash.status.${c.status}`)}
                    </Badge>
                  </span>
                  <span className="block text-xs text-muted mt-[3px]">
                    {formatDateTime(c.startsAt, { locale })} →{' '}
                    {formatDateTime(c.endsAt, { locale })}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      </section>
      {detail.data ? (
        <Detail c={detail.data} />
      ) : selId != null ? (
        <div className="bg-surface-card rounded-lg shadow-sm p-6">
          <Skeleton lines={6} />
        </div>
      ) : null}
      <CreateModal open={creating} onClose={() => setCreating(false)} onCreated={select} />
    </div>
  );
}

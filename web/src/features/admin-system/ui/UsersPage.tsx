// Người dùng (prototype admin/Users.jsx) — plan G12.42: tìm email / SĐT / tên shop, tab Tất cả / Khách
// hàng / Shop / Đang khóa (?tab=), bảng Tài khoản / Loại / SĐT / Số đơn / Cảnh báo / Trạng thái (+ lý do ·
// thời hạn); Khóa tài khoản (lý do 16.4, thời hạn 7 / 30 ngày / vô thời hạn), Mở khóa (lý do bắt buộc).
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useSearchParams } from 'react-router-dom';
import { formatPhone } from '@/features/account';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatNumber } from '@/shared/lib/format';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Avatar,
  Badge,
  Button,
  EmptyState,
  Icon,
  Input,
  Modal,
  Select,
  Skeleton,
  Tabs,
  Textarea,
  toast,
  type BadgeVariant,
} from '@/shared/ui';
import { useReactivateUser, useSuspendUser, useUsers } from '../application/useSystem';
import {
  DURATIONS,
  SUSPEND_REASONS,
  USER_TABS,
  type Duration,
  type SuspendReason,
} from '../domain/system.rules';
import {
  reactivateSchema,
  suspendSchema,
  type ReactivateValues,
  type SuspendValues,
} from '../domain/system.schema';
import type { AccountState, PlatformUser, UserTab } from '../domain/system.types';

const ST: Record<AccountState, BadgeVariant> = {
  ACTIVE: 'success',
  SUSPENDED: 'warning',
  LOCKED: 'danger',
};
const th = 'px-4 py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-4 py-[11px] whitespace-nowrap align-middle';
const nameOf = (u: PlatformUser) => u.shopName ?? u.fullName;

function SuspendModal({ u, onClose }: { u: PlatformUser | null; onClose: () => void }) {
  const { t } = useT();
  const suspend = useSuspendUser();
  const form = useForm<SuspendValues>({
    resolver: zodResolver(suspendSchema),
    defaultValues: { reason: 'COUNTERFEIT_SELLER', duration: 'P7D', note: '' },
  });
  useEffect(() => {
    if (u)
      form.reset({
        reason: u.role === 'VENDOR' ? 'COUNTERFEIT_SELLER' : 'ABNORMAL_RETURNS',
        duration: 'P7D',
        note: '',
      });
  }, [u, form]);
  const v = form.watch();
  const err = form.formState.errors.note?.message;
  const submit = form.handleSubmit(
    (x) =>
      u &&
      suspend.mutate(
        {
          id: u.id,
          reason: x.reason,
          note: x.note || null,
          duration: x.duration,
          label: t(`admin-system.users.reasons.${x.reason}`),
        },
        {
          onSuccess: () => {
            toast.success(t('admin-system.users.suspended', { name: nameOf(u) }));
            onClose();
          },
          onError: (e) => toast.error(errorMessage(e, t)),
        },
      ),
  );
  return (
    <Modal
      open={!!u}
      onClose={onClose}
      width={480}
      title={u ? t('admin-system.users.lock_title', { name: nameOf(u) }) : ''}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant="danger"
            iconLeft="lock"
            loading={suspend.isPending}
            onClick={() => void submit()}
          >
            {t('admin-system.users.lock_confirm')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-3.5" onSubmit={(ev) => void submit(ev)} noValidate>
        <Select<SuspendReason>
          label={t('admin-system.users.reason')}
          value={v.reason}
          onChange={(x) =>
            form.setValue('reason', x, { shouldValidate: form.formState.isSubmitted })
          }
          options={SUSPEND_REASONS.map((r) => ({
            value: r,
            label: t(`admin-system.users.reasons.${r}`),
          }))}
        />
        <Select<Duration>
          label={t('admin-system.users.duration')}
          value={v.duration}
          onChange={(x) => form.setValue('duration', x)}
          options={DURATIONS.map((d) => ({
            value: d,
            label: t(`admin-system.users.durations.${d}`),
          }))}
        />
        {v.reason === 'OTHER' && (
          <Textarea
            label={t('admin-system.users.note')}
            rows={3}
            maxLength={500}
            required
            error={err ? t(err) : undefined}
            {...form.register('note')}
          />
        )}
        <div className="flex gap-2 text-xs text-muted leading-normal">
          <Icon name="info" size={15} className="flex-none text-blue-600" />
          {t('admin-system.users.lock_hint')}
        </div>
      </form>
    </Modal>
  );
}

function ReactivateModal({ u, onClose }: { u: PlatformUser | null; onClose: () => void }) {
  const { t } = useT();
  const act = useReactivateUser();
  const form = useForm<ReactivateValues>({
    resolver: zodResolver(reactivateSchema),
    defaultValues: { reason: '' },
  });
  useEffect(() => {
    if (u) form.reset({ reason: '' });
  }, [u, form]);
  const err = form.formState.errors.reason?.message;
  const submit = form.handleSubmit(
    (x) =>
      u &&
      act.mutate(
        { id: u.id, reason: x.reason },
        {
          onSuccess: () => {
            toast.success(t('admin-system.users.reactivated', { name: nameOf(u) }));
            onClose();
          },
          onError: (e) => toast.error(errorMessage(e, t)),
        },
      ),
  );
  return (
    <Modal
      open={!!u}
      onClose={onClose}
      width={440}
      title={u ? t('admin-system.users.unlock_title', { name: nameOf(u) }) : ''}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button iconLeft="check" loading={act.isPending} onClick={() => void submit()}>
            {t('admin-system.users.unlock')}
          </Button>
        </>
      }
    >
      <form onSubmit={(ev) => void submit(ev)} noValidate>
        <Textarea
          label={t('admin-system.users.unlock_reason')}
          rows={3}
          maxLength={500}
          required
          error={err ? t(err) : undefined}
          {...form.register('reason')}
        />
      </form>
    </Modal>
  );
}

export default function UsersPage() {
  const { t, locale } = useT();
  const [params, setParams] = useSearchParams();
  const raw = params.get('tab') as UserTab | null;
  const tab: UserTab = raw && USER_TABS.includes(raw) ? raw : 'ALL';
  const [q, setQ] = useState('');
  const [term, setTerm] = useState('');
  useEffect(() => {
    const id = setTimeout(() => setTerm(q.trim()), 250);
    return () => clearTimeout(id);
  }, [q]);
  const list = useUsers(tab, term);
  const [locking, setLocking] = useState<PlatformUser | null>(null);
  const [unlocking, setUnlocking] = useState<PlatformUser | null>(null);
  if (list.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(list.error, t)}
      />
    );
  const counts = list.data?.counts;
  const rows = list.data?.items ?? [];
  return (
    <div className="max-w-[1180px]">
      <div className="flex items-center gap-3.5 mb-3.5 flex-wrap">
        <Input
          aria-label={t('admin-system.users.search')}
          wrapperClassName="flex-1 min-w-[280px] max-w-[440px] max-sm:min-w-full"
          iconLeft="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={t('admin-system.users.search_placeholder')}
        />
        <span className="ml-auto text-sm text-muted max-sm:ml-0">
          {t('admin-system.users.staff_note_before')}
          <b>{t('admin.shell.nav.rbac')}</b>.
        </span>
      </div>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-hidden">
        <div className="px-4 pt-1 overflow-x-auto">
          <Tabs
            value={tab}
            onChange={(x) => setParams({ tab: x }, { replace: true })}
            aria-label={t('admin-system.users.tabs_label')}
            items={USER_TABS.map((x) => ({
              id: x,
              label: t(`admin-system.users.tabs.${x}`),
              badge: counts && (x !== 'LOCKED' || counts.LOCKED > 0) ? counts[x] : undefined,
            }))}
          />
        </div>
        <div className="overflow-x-auto">
          {list.isPending ? (
            <Skeleton lines={5} className="m-5" />
          ) : (
            <table className="w-full min-w-[880px] border-collapse tabular-nums">
              <thead>
                <tr>
                  <th className={th}>{t('admin-system.users.col.account')}</th>
                  <th className={th}>{t('admin-system.users.col.type')}</th>
                  <th className={th}>{t('admin-system.users.col.phone')}</th>
                  <th className={cn(th, 'text-right')}>{t('admin-system.users.col.orders')}</th>
                  <th className={th}>{t('admin-system.users.col.warnings')}</th>
                  <th className={th}>{t('admin-system.users.col.status')}</th>
                  <th className={cn(th, 'text-right')}>{t('admin-system.users.col.action')}</th>
                </tr>
              </thead>
              <tbody>
                {rows.length === 0 && (
                  <tr>
                    <td colSpan={7} className="p-10 text-center text-base text-muted">
                      {t('admin-system.users.empty')}
                    </td>
                  </tr>
                )}
                {rows.map((u) => (
                  <tr
                    key={u.id}
                    className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                  >
                    <td className={td}>
                      <div className="flex items-center gap-2.5">
                        <Avatar name={nameOf(u)} size={34} square={u.role === 'VENDOR'} />
                        <div>
                          <div className="font-semibold text-strong">{nameOf(u)}</div>
                          <div className="font-mono text-[11px] text-muted">{u.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className={td}>
                      <Badge variant={u.role === 'VENDOR' ? 'primary' : 'neutral'}>
                        {t(`admin-system.users.roles.${u.role}`)}
                      </Badge>
                    </td>
                    <td className={cn(td, 'font-mono')}>{formatPhone(u.phone)}</td>
                    <td className={cn(td, 'text-right')}>
                      {formatNumber(u.orderCount, { locale })}
                    </td>
                    <td className={td}>
                      {u.warningCount > 0 ? (
                        <Badge variant="danger" icon="triangle-alert">
                          {t('admin-system.users.warnings', { count: u.warningCount })}
                        </Badge>
                      ) : (
                        <span className="text-subtle">—</span>
                      )}
                    </td>
                    <td className={td}>
                      <Badge variant={ST[u.status]}>
                        {t(`admin-system.users.status.${u.status}`)}
                      </Badge>
                      {u.suspension && (
                        <div className="text-xs text-muted mt-[3px] max-w-[220px] whitespace-normal">
                          {u.suspension.reason} ·{' '}
                          {u.suspension.until
                            ? t('admin-system.users.until', {
                                date: formatDate(u.suspension.until, { locale }),
                              })
                            : t('admin-system.users.durations.FOREVER')}
                        </div>
                      )}
                    </td>
                    <td className={cn(td, 'text-right')}>
                      {u.status === 'ACTIVE' ? (
                        <Button
                          size="sm"
                          variant="ghost"
                          iconLeft="lock"
                          className="!text-red-600"
                          onClick={() => setLocking(u)}
                        >
                          {t('admin-system.users.lock')}
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          variant="outline"
                          iconLeft="check"
                          onClick={() => setUnlocking(u)}
                        >
                          {t('admin-system.users.unlock')}
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
      <SuspendModal u={locking} onClose={() => setLocking(null)} />
      <ReactivateModal u={unlocking} onClose={() => setUnlocking(null)} />
    </div>
  );
}

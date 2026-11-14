// Thông báo sàn (prototype admin/PlatformNotifications.jsx) — plan G12.37: 3 chỉ số, tìm tiêu đề / nội
// dung, lọc đối tượng, bảng Mã TB / Thông báo / Ảnh / Đối tượng / Người gửi / Thời gian / Đã đọc (%) hoặc
// "Đã hẹn giờ" (5 dòng / trang); chi tiết (Sửa khi chưa phát / Xóa); soạn (đối tượng, email người nhận cụ
// thể, tiêu đề ≤ 100, nội dung rich-text, ảnh, hẹn giờ) → xác nhận phát.
import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useAdminMe } from '@/features/admin-system';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { uploadFile, UploadError } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime, formatNumber } from '@/shared/lib/format';
import { maxMb } from '@/shared/lib/upload.rules';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Badge,
  Button,
  ConfirmDialog,
  EmptyState,
  Icon,
  Input,
  Modal,
  Radio,
  Select,
  Skeleton,
  Spinner,
  TablePager,
  toast,
  type BadgeVariant,
  type IconName,
} from '@/shared/ui';
import {
  useDeletePlatformNotification,
  usePlatformNotification,
  usePlatformNotifications,
  useSavePlatformNotification,
} from '../application/useMarketing';
import {
  NOTIFICATION_PAGE_SIZE,
  NOTIFICATION_TARGETS,
  parseEmails,
  readRate,
  TITLE_MAX,
} from '../domain/marketing.rules';
import { notificationSchema, type NotificationValues } from '../domain/marketing.schema';
import type { NotificationTarget, PlatformNotificationDetail } from '../domain/marketing.types';
import { RichBox, sanitizeHtml } from './RichBox';

const T_VARIANT: Record<NotificationTarget, BadgeVariant> = {
  ALL: 'primary',
  VENDOR: 'warning',
  CUSTOMER: 'mint',
  SPECIFIC_USERS: 'neutral',
};
const th =
  'px-[18px] py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-[18px] py-[11px] whitespace-nowrap';
const lab = 'block text-sm font-semibold text-strong';

function Stat({
  icon,
  tone,
  label,
  value,
}: {
  icon: IconName;
  tone: [string, string];
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center gap-3.5 bg-surface-card rounded-lg shadow-sm px-[18px] py-4">
      <span
        className="w-[42px] h-[42px] rounded-md grid place-items-center flex-none"
        style={{ background: tone[0], color: tone[1] }}
      >
        <Icon name={icon} size={20} />
      </span>
      <div>
        <div className="text-[13px] font-medium text-muted">{label}</div>
        <div className="text-2xl font-extrabold text-strong tabular-nums max-md:text-lg">
          {value}
        </div>
      </div>
    </div>
  );
}

function ComposeModal({
  editing,
  open,
  onClose,
}: {
  editing: PlatformNotificationDetail | null;
  open: boolean;
  onClose: () => void;
}) {
  const { t } = useT();
  const me = useAdminMe();
  const save = useSavePlatformNotification();
  const fileRef = useRef<HTMLInputElement>(null);
  const [img, setImg] = useState<{ key: string | null; preview: string | null }>({
    key: null,
    preview: null,
  });
  const [uploading, setUploading] = useState(false);
  const [confirm, setConfirm] = useState<NotificationValues | null>(null);
  const [richKey, setRichKey] = useState(0);
  const [unknown, setUnknown] = useState('');
  const form = useForm<NotificationValues>({
    resolver: zodResolver(notificationSchema),
    defaultValues: { target: 'ALL', emailsText: '', title: '', contentHtml: '', sendAt: '' },
  });
  useEffect(() => {
    if (!open) return;
    form.reset(
      editing
        ? {
            target: editing.target,
            emailsText: editing.recipientEmails.join(', '),
            title: editing.title,
            contentHtml: editing.contentHtml,
            sendAt: editing.scheduled ? editing.sendAt.slice(0, 16) : '',
          }
        : { target: 'ALL', emailsText: '', title: '', contentHtml: '', sendAt: '' },
    );
    setImg({ key: null, preview: editing?.imageUrl ?? null });
    setRichKey((k) => k + 1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, editing]);
  const v = form.watch();
  const e = form.formState.errors;
  const m = (x?: string) => (x ? t(x) : undefined);

  const pick = async (f: File) => {
    setUploading(true);
    try {
      const r = await uploadFile(f, 'NOTIFICATION_IMAGE');
      setImg({ key: r.objectKey, preview: r.previewUrl });
    } catch (err) {
      toast.error(
        err instanceof UploadError
          ? t(`admin-marketing.notif.err.upload_${err.issue}`, { mb: maxMb('NOTIFICATION_IMAGE') })
          : errorMessage(err, t),
      );
    } finally {
      setUploading(false);
    }
  };

  const ask = form.handleSubmit((vals) => {
    if (vals.target === 'SPECIFIC_USERS') {
      const { invalid } = parseEmails(vals.emailsText);
      if (invalid.length) {
        form.setError('emailsText', { message: 'admin-marketing.notif.err.email_format' });
        return;
      }
    }
    setConfirm(vals);
  });

  const send = () => {
    if (!confirm) return;
    save.mutate(
      {
        id: editing?.id ?? null,
        sender: me.data?.fullName ?? 'Admin',
        input: {
          title: confirm.title,
          contentHtml: sanitizeHtml(confirm.contentHtml),
          imageObjectKey: img.key,
          imagePreview: img.preview,
          target: confirm.target,
          recipientEmails: parseEmails(confirm.emailsText).emails,
          sendAt: confirm.sendAt || null,
        },
      },
      {
        onSuccess: () => {
          toast.success(
            t(
              editing
                ? 'admin-marketing.notif.updated'
                : confirm.sendAt
                  ? 'admin-marketing.notif.scheduled_ok'
                  : 'admin-marketing.notif.sent',
            ),
          );
          setConfirm(null);
          onClose();
        },
        onError: (err) => {
          setConfirm(null);
          if (isApiError(err) && err.code === 'GNE-NOT-4221') {
            setUnknown(String(err.details.unknown_emails ?? ''));
            form.setError('emailsText', { message: 'admin-marketing.notif.err.unknown' });
          } else toast.error(errorMessage(err, t));
        },
      },
    );
  };

  return (
    <>
      <Modal
        open={open}
        onClose={onClose}
        width={560}
        dismissible={!form.formState.isDirty}
        title={
          editing ? t('admin-marketing.notif.edit_title') : t('admin-marketing.notif.compose_title')
        }
        footer={
          <>
            <Button variant="ghost" onClick={onClose}>
              {t('common.action.cancel')}
            </Button>
            <Button iconLeft="send" disabled={uploading} onClick={() => void ask()}>
              {editing
                ? t('admin-marketing.notif.update')
                : v.sendAt
                  ? t('admin-marketing.notif.schedule')
                  : t('admin-marketing.notif.broadcast')}
            </Button>
          </>
        }
      >
        <form className="flex flex-col gap-3.5" onSubmit={(ev) => void ask(ev)} noValidate>
          <fieldset className="border-0 p-0 m-0">
            <legend className={lab}>{t('admin-marketing.notif.target')}</legend>
            <div className="flex gap-x-4 gap-y-1 mt-2 flex-wrap">
              {NOTIFICATION_TARGETS.map((k) => (
                <Radio
                  key={k}
                  name="ntarget"
                  checked={v.target === k}
                  onChange={() => form.setValue('target', k, { shouldDirty: true })}
                  className="text-sm"
                >
                  {t(`admin-marketing.notif.targets.${k}`)}
                </Radio>
              ))}
            </div>
          </fieldset>
          {v.target === 'SPECIFIC_USERS' && (
            <Input
              label={t('admin-marketing.notif.emails')}
              placeholder="mai.tran@gmail.com, an.nguyen@gmail.com"
              error={
                e.emailsText?.message ? t(e.emailsText.message, { emails: unknown }) : undefined
              }
              {...form.register('emailsText')}
            />
          )}
          <Input
            label={t('admin-marketing.notif.title')}
            maxLength={TITLE_MAX}
            placeholder={t('admin-marketing.notif.title_placeholder')}
            error={m(e.title?.message)}
            {...form.register('title')}
          />
          <div>
            <span className={lab}>{t('admin-marketing.notif.image')}</span>
            <input
              ref={fileRef}
              type="file"
              hidden
              accept="image/jpeg,image/png,image/webp"
              onChange={(ev) => {
                const f = ev.target.files?.[0];
                ev.target.value = '';
                if (f) void pick(f);
              }}
            />
            {img.preview ? (
              <div className="relative mt-2">
                <img
                  src={img.preview}
                  alt={t('admin-marketing.notif.image_preview')}
                  width={520}
                  height={180}
                  className="w-full max-h-[180px] object-cover rounded-md"
                />
                <button
                  type="button"
                  aria-label={t('admin-marketing.notif.remove_image')}
                  onClick={() => setImg({ key: null, preview: null })}
                  className="absolute top-2 right-2 w-7 h-7 rounded-full border-0 bg-black/60 text-white cursor-pointer grid place-items-center"
                >
                  <Icon name="x" size={16} />
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => fileRef.current?.click()}
                className="mt-2 w-full py-[22px] text-center border-[1.5px] border-dashed border-line-strong rounded-md bg-gray-25 cursor-pointer text-muted focus-visible:outline-none focus-visible:shadow-focus"
              >
                {uploading ? (
                  <Spinner size={22} />
                ) : (
                  <>
                    <Icon name="upload" size={24} />
                    <span className="block text-sm mt-1.5">
                      {t('admin-marketing.notif.pick_image')}
                    </span>
                    <span className="block text-xs text-subtle">
                      {t('admin-marketing.notif.image_hint', { mb: maxMb('NOTIFICATION_IMAGE') })}
                    </span>
                  </>
                )}
              </button>
            )}
          </div>
          <div>
            <span className={lab}>{t('admin-marketing.notif.content')}</span>
            <RichBox
              key={richKey}
              initial={editing?.contentHtml ?? ''}
              label={t('admin-marketing.notif.content')}
              invalid={!!e.contentHtml}
              onChange={(h) =>
                form.setValue('contentHtml', h, {
                  shouldDirty: true,
                  shouldValidate: form.formState.isSubmitted,
                })
              }
            />
            {e.contentHtml && (
              <p role="alert" className="m-0 mt-1.5 text-xs text-red-600">
                {m(e.contentHtml.message)}
              </p>
            )}
          </div>
          {(!editing || editing.scheduled) && (
            <div>
              <label className={lab} htmlFor="n-send-at">
                {t('admin-marketing.notif.send_at')}
              </label>
              <input
                id="n-send-at"
                type="datetime-local"
                className="block mt-1.5 h-[42px] border-[1.5px] border-solid border-line rounded-md px-2.5 text-sm bg-surface-card text-body outline-none focus:border-primary"
                {...form.register('sendAt')}
              />
            </div>
          )}
        </form>
      </Modal>
      <ConfirmDialog
        open={!!confirm}
        title={t(
          editing ? 'admin-marketing.notif.confirm_update' : 'admin-marketing.notif.confirm_send',
        )}
        message={t('admin-marketing.notif.confirm_desc', {
          target: confirm ? t(`admin-marketing.notif.targets.${confirm.target}`) : '',
        })}
        confirmLabel={t(
          editing
            ? 'admin-marketing.notif.confirm_update_btn'
            : 'admin-marketing.notif.confirm_send_btn',
        )}
        cancelLabel={t('admin-marketing.notif.back')}
        loading={save.isPending}
        onCancel={() => setConfirm(null)}
        onConfirm={send}
      />
    </>
  );
}

function DetailModal({
  id,
  onClose,
  onEdit,
  onDelete,
}: {
  id: number | null;
  onClose: () => void;
  onEdit: (d: PlatformNotificationDetail) => void;
  onDelete: (d: PlatformNotificationDetail) => void;
}) {
  const { t, locale } = useT();
  const q = usePlatformNotification(id);
  const d = q.data;
  return (
    <Modal
      open={id != null}
      onClose={onClose}
      width={500}
      title={t('admin-marketing.notif.detail')}
      footer={
        d && (
          <>
            <Button
              variant="ghost"
              iconLeft="trash-2"
              className="mr-auto !text-red-600"
              onClick={() => onDelete(d)}
            >
              {t('admin-marketing.notif.delete')}
            </Button>
            {d.scheduled && (
              <Button variant="outline" iconLeft="pencil" onClick={() => onEdit(d)}>
                {t('admin-marketing.notif.edit')}
              </Button>
            )}
            <Button onClick={onClose}>{t('common.action.close')}</Button>
          </>
        )
      }
    >
      {!d ? (
        <Skeleton lines={5} />
      ) : (
        <div className="flex flex-col gap-3">
          <div>
            <div className="text-xs text-muted mb-[3px]">{t('admin-marketing.notif.title')}</div>
            <div className="text-base font-semibold text-strong">{d.title}</div>
          </div>
          <div className="flex gap-6 flex-wrap">
            <div>
              <div className="text-xs text-muted mb-[3px]">
                {t('admin-marketing.notif.col.target')}
              </div>
              <Badge variant={T_VARIANT[d.target]}>
                {t(`admin-marketing.notif.targets.${d.target}`)}
              </Badge>
            </div>
            <div>
              <div className="text-xs text-muted mb-[3px]">
                {t('admin-marketing.notif.col.time')}
              </div>
              <div className="text-sm text-body">{formatDateTime(d.sendAt, { locale })}</div>
            </div>
            <div>
              <div className="text-xs text-muted mb-[3px]">
                {t('admin-marketing.notif.read_of')}
              </div>
              <div className="text-sm text-body">
                {formatNumber(d.readCount, { locale })} / {formatNumber(d.reachCount, { locale })} (
                {readRate(d)}%)
              </div>
            </div>
          </div>
          {d.recipientEmails.length > 0 && (
            <div className="text-sm text-body">
              <span className="text-xs text-muted block mb-[3px]">
                {t('admin-marketing.notif.emails')}
              </span>
              {d.recipientEmails.join(', ')}
            </div>
          )}
          {d.imageUrl && (
            <img
              src={d.imageUrl}
              alt=""
              width={460}
              height={200}
              className="w-full h-auto rounded-md"
            />
          )}
          <div>
            <div className="text-xs text-muted mb-[3px]">{t('admin-marketing.notif.content')}</div>
            <div
              className="text-base text-body leading-relaxed [&_a]:text-link [&_p]:m-0"
              dangerouslySetInnerHTML={{ __html: sanitizeHtml(d.contentHtml) }}
            />
          </div>
        </div>
      )}
    </Modal>
  );
}

const compact = (n: number) =>
  n < 1000
    ? String(n)
    : n < 1e6
      ? `~${(n / 1e3).toFixed(1).replace(/\.0$/, '')}K`
      : `~${(n / 1e6).toFixed(1).replace(/\.0$/, '')}M`;

export default function PlatformNotificationsPage() {
  const { t, locale } = useT();
  const [q, setQ] = useState('');
  const [term, setTerm] = useState('');
  const [target, setTarget] = useState<NotificationTarget | 'ALL_TARGETS'>('ALL_TARGETS');
  const [page, setPage] = useState(1);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [compose, setCompose] = useState<{ editing: PlatformNotificationDetail | null } | null>(
    null,
  );
  const [deleting, setDeleting] = useState<PlatformNotificationDetail | null>(null);
  const del = useDeletePlatformNotification();
  useEffect(() => {
    const id = setTimeout(() => {
      setTerm(q.trim());
      setPage(1);
    }, 250);
    return () => clearTimeout(id);
  }, [q]);
  const list = usePlatformNotifications(target === 'ALL_TARGETS' ? null : target, term, page);
  if (list.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(list.error, t)}
      />
    );
  const data = list.data;
  return (
    <div className="max-w-[1180px]">
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div>
          <h2 className="m-0 text-xl font-bold text-strong">
            {t('admin-marketing.notif.heading')}
          </h2>
          <p className="m-0 mt-1 text-sm text-muted">{t('admin-marketing.notif.sub')}</p>
        </div>
        <Button iconLeft="send" className="ml-auto" onClick={() => setCompose({ editing: null })}>
          {t('admin-marketing.notif.create')}
        </Button>
      </div>
      <div className="grid grid-cols-3 gap-4 mb-4 max-md:grid-cols-1">
        <Stat
          icon="send"
          tone={['var(--blue-50)', 'var(--blue-600)']}
          label={t('admin-marketing.notif.kpi.sent')}
          value={data ? String(data.summary.sentThisMonth) : '…'}
        />
        <Stat
          icon="user"
          tone={['var(--mint-50)', 'var(--mint-600)']}
          label={t('admin-marketing.notif.kpi.reach')}
          value={data ? compact(data.summary.totalReach) : '…'}
        />
        <Stat
          icon="eye"
          tone={['var(--brand-50)', 'var(--brand-600)']}
          label={t('admin-marketing.notif.kpi.reads')}
          value={data ? formatNumber(data.summary.totalReads, { locale }) : '…'}
        />
      </div>
      <div className="flex items-center gap-3 mb-3 flex-wrap">
        <Input
          aria-label={t('admin-marketing.notif.search')}
          wrapperClassName="flex-1 min-w-[220px]"
          iconLeft="search"
          value={q}
          onChange={(ev) => setQ(ev.target.value)}
          placeholder={t('admin-marketing.notif.search_placeholder')}
        />
        <Select<NotificationTarget | 'ALL_TARGETS'>
          className="w-[200px] max-sm:w-full"
          aria-label={t('admin-marketing.notif.filter')}
          value={target}
          onChange={(x) => {
            setTarget(x);
            setPage(1);
          }}
          options={[
            { value: 'ALL_TARGETS', label: t('admin-marketing.notif.all_targets') },
            ...(['VENDOR', 'CUSTOMER', 'SPECIFIC_USERS', 'ALL'] as const).map((x) => ({
              value: x,
              label: t(`admin-marketing.notif.targets.${x}`),
            })),
          ]}
        />
      </div>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          {!data ? (
            <Skeleton lines={5} className="m-5" />
          ) : (
            <table className="w-full min-w-[860px] border-collapse tabular-nums">
              <thead>
                <tr>
                  <th className={th}>{t('admin-marketing.notif.col.code')}</th>
                  <th className={th}>{t('admin-marketing.notif.col.notification')}</th>
                  <th className={th}>{t('admin-marketing.notif.col.image')}</th>
                  <th className={th}>{t('admin-marketing.notif.col.target')}</th>
                  <th className={th}>{t('admin-marketing.notif.col.by')}</th>
                  <th className={th}>{t('admin-marketing.notif.col.time')}</th>
                  <th className={cn(th, 'text-right')}>{t('admin-marketing.notif.col.read')}</th>
                </tr>
              </thead>
              <tbody>
                {data.items.length === 0 && (
                  <tr>
                    <td colSpan={7} className="p-10 text-center text-base text-muted">
                      {t('admin-marketing.notif.empty')}
                    </td>
                  </tr>
                )}
                {data.items.map((n) => (
                  <tr
                    key={n.id}
                    className="border-0 border-t border-solid border-line-subtle text-[13px] text-body hover:bg-gray-25"
                  >
                    <td className={cn(td, 'font-mono text-xs')}>{n.code}</td>
                    <td className={cn(td, 'max-w-[320px]')}>
                      <button
                        type="button"
                        onClick={() => setDetailId(n.id)}
                        className="block w-full text-left border-0 bg-transparent p-0 cursor-pointer focus-visible:outline-none focus-visible:shadow-focus rounded-xs"
                      >
                        <span className="block font-semibold text-strong truncate">{n.title}</span>
                        <span className="block text-xs text-muted truncate">
                          {n.contentPreview}
                        </span>
                      </button>
                    </td>
                    <td className={td}>
                      {n.imageUrl ? (
                        <img
                          src={n.imageUrl}
                          alt=""
                          width={36}
                          height={36}
                          className="w-9 h-9 rounded-sm object-cover"
                        />
                      ) : (
                        <span className="w-9 h-9 rounded-sm bg-gray-100 text-subtle grid place-items-center">
                          <Icon name="image" size={16} />
                        </span>
                      )}
                    </td>
                    <td className={td}>
                      <Badge variant={T_VARIANT[n.target]}>
                        {t(`admin-marketing.notif.targets.${n.target}`)}
                      </Badge>
                    </td>
                    <td className={td}>{n.createdBy}</td>
                    <td className={cn(td, 'text-muted')}>{formatDateTime(n.sendAt, { locale })}</td>
                    <td className={cn(td, 'text-right')}>
                      {n.scheduled ? (
                        <Badge variant="primary">{t('admin-marketing.notif.scheduled')}</Badge>
                      ) : (
                        <>
                          {formatNumber(n.readCount, { locale })}{' '}
                          <span className="text-muted">({readRate(n)}%)</span>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        {data && data.total_items > 0 && (
          <TablePager
            page={data.page}
            size={NOTIFICATION_PAGE_SIZE}
            totalItems={data.total_items}
            totalPages={data.total_pages}
            onChange={setPage}
            unit={t('admin-marketing.notif.unit')}
          />
        )}
      </div>
      <DetailModal
        id={detailId}
        onClose={() => setDetailId(null)}
        onEdit={(d) => {
          setDetailId(null);
          setCompose({ editing: d });
        }}
        onDelete={(d) => {
          setDetailId(null);
          setDeleting(d);
        }}
      />
      <ComposeModal
        open={!!compose}
        editing={compose?.editing ?? null}
        onClose={() => setCompose(null)}
      />
      <ConfirmDialog
        open={!!deleting}
        tone="danger"
        title={t('admin-marketing.notif.delete_title')}
        message={t('admin-marketing.notif.delete_desc')}
        confirmLabel={t('admin-marketing.notif.delete_confirm')}
        loading={del.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() =>
          deleting &&
          del.mutate(deleting.id, {
            onSuccess: () => {
              toast.success(t('admin-marketing.notif.deleted'));
              setDeleting(null);
            },
            onError: (err) => toast.error(errorMessage(err, t)),
          })
        }
      />
    </div>
  );
}

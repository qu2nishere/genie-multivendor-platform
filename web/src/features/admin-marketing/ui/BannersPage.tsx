// Banner trang chủ (prototype admin/Banners.jsx) — plan G12.36: lọc vị trí (?position=), bảng Banner /
// Vị trí / Thời gian / Hiển thị – Bấm / CTR / Trạng thái; Xem (mở trang storefront), Sửa, Tắt ngay /
// Bật lại, Xóa (chỉ khi đã tắt hoặc kết thúc); Thêm banner.
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatNumber, formatPercent } from '@/shared/lib/format';
import {
  Badge,
  Button,
  ConfirmDialog,
  EmptyState,
  Icon,
  Skeleton,
  Tabs,
  toast,
  type BadgeVariant,
} from '@/shared/ui';
import { useBanners, useDeleteBanner, useSetBannerActive } from '../application/useMarketing';
import {
  BANNER_POSITIONS,
  canDeleteBanner,
  ctr,
  parseLink,
  previewPath,
} from '../domain/marketing.rules';
import type { AdminBanner, BannerPosition, BannerState } from '../domain/marketing.types';
import { BannerModal } from './BannerModal';

const ST: Record<BannerState, BadgeVariant> = {
  LIVE: 'success',
  SCHEDULED: 'primary',
  ENDED: 'neutral',
  OFF: 'danger',
};
const th = 'px-4 py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-4 py-[11px] whitespace-nowrap';

export default function BannersPage() {
  const { t, locale } = useT();
  const [params, setParams] = useSearchParams();
  const raw = params.get('position') as BannerPosition | null;
  const pos = raw && BANNER_POSITIONS.includes(raw) ? raw : null;
  const all = useBanners(null);
  const toggle = useSetBannerActive();
  const del = useDeleteBanner();
  const [modal, setModal] = useState<{ editing: AdminBanner | null } | null>(null);
  const [deleting, setDeleting] = useState<AdminBanner | null>(null);
  if (all.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(all.error, t)}
      />
    );
  const rows = all.data ?? [];
  const list = rows
    .filter((b) => !pos || b.position === pos)
    .sort(
      (a, b) =>
        BANNER_POSITIONS.indexOf(a.position) - BANNER_POSITIONS.indexOf(b.position) ||
        (a.category?.id ?? 0) - (b.category?.id ?? 0) ||
        a.sortOrder - b.sortOrder,
    );
  const target = (b: AdminBanner) => {
    const p = parseLink(b.targetUrl);
    return `${t(`admin-marketing.banner.link.types.${p.type}`)}${p.value ? `: ${p.value}` : ''}`;
  };
  return (
    <div className="max-w-[1180px]">
      <div className="flex items-center gap-3 mb-3.5 flex-wrap">
        <div className="flex-1 min-w-0 overflow-x-auto">
          <Tabs
            value={pos ?? 'ALL'}
            onChange={(v) => setParams(v === 'ALL' ? {} : { position: v }, { replace: true })}
            aria-label={t('admin-marketing.banner.position')}
            items={[
              { id: 'ALL', label: t('admin-marketing.banner.all') },
              ...BANNER_POSITIONS.map((p) => ({
                id: p,
                label: t(`admin-marketing.banner.positions.${p}.label`),
              })),
            ]}
          />
        </div>
        <Button iconLeft="plus" onClick={() => setModal({ editing: null })}>
          {t('admin-marketing.banner.add')}
        </Button>
      </div>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-x-auto">
        {all.isPending ? (
          <Skeleton lines={6} className="m-5" />
        ) : (
          <table className="w-full min-w-[900px] border-collapse tabular-nums">
            <thead>
              <tr>
                <th className={th}>{t('admin-marketing.banner.col.banner')}</th>
                <th className={th}>{t('admin-marketing.banner.col.position')}</th>
                <th className={th}>{t('admin-marketing.banner.col.time')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-marketing.banner.col.views')}</th>
                <th className={cn(th, 'text-right')}>CTR</th>
                <th className={th}>{t('admin-marketing.banner.col.status')}</th>
                <th
                  className={cn(
                    th,
                    'text-right sticky right-0 shadow-[-6px_0_8px_-6px_rgba(0,0,0,.12)]',
                  )}
                >
                  {t('admin-marketing.banner.col.action')}
                </th>
              </tr>
            </thead>
            <tbody>
              {list.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-10 text-center text-base text-muted">
                    {t('admin-marketing.banner.empty')}
                  </td>
                </tr>
              )}
              {list.map((b) => {
                const rate = ctr(b);
                return (
                  <tr
                    key={b.id}
                    className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                  >
                    <td className={td}>
                      <div className="flex items-center gap-3">
                        <img
                          src={b.imageUrl}
                          alt=""
                          width={84}
                          height={44}
                          className="w-[84px] h-11 rounded-sm object-cover flex-none"
                        />
                        <div className="max-w-[240px] min-w-0">
                          <div className="font-semibold text-strong truncate">{b.title}</div>
                          <div className="text-xs text-muted truncate">{target(b)}</div>
                        </div>
                      </div>
                    </td>
                    <td className={td}>
                      {t(`admin-marketing.banner.positions.${b.position}.label`)}
                      {b.category && <span className="text-muted"> · {b.category.name}</span>}
                      <div className="max-w-[220px] whitespace-normal text-xs text-muted">
                        {t(`admin-marketing.banner.positions.${b.position}.where`)}
                      </div>
                    </td>
                    <td className={cn(td, 'text-muted text-xs')}>
                      {formatDate(b.startsAt, { locale })}
                      <br />→ {formatDate(b.endsAt, { locale })}
                    </td>
                    <td className={cn(td, 'text-right')}>
                      {formatNumber(b.impressions, { locale })} /{' '}
                      {formatNumber(b.clicks, { locale })}
                    </td>
                    <td className={cn(td, 'text-right font-semibold')}>
                      {rate === null ? '—' : formatPercent(rate, { locale, digits: 1 })}
                    </td>
                    <td className={td}>
                      <Badge variant={ST[b.state]}>
                        {t(`admin-marketing.banner.state.${b.state}`)}
                      </Badge>
                    </td>
                    <td
                      className={cn(
                        td,
                        'text-right sticky right-0 bg-surface-card shadow-[-6px_0_8px_-6px_rgba(0,0,0,.12)]',
                      )}
                    >
                      <div className="flex gap-1 justify-end items-center">
                        <a
                          href={previewPath(b)}
                          target="_blank"
                          rel="noopener"
                          className="inline-flex items-center gap-1.5 h-8 px-2.5 rounded-md text-link font-semibold text-[13px] no-underline hover:underline"
                        >
                          <Icon name="external-link" size={14} />
                          {t('admin-marketing.banner.view')}
                        </a>
                        <Button
                          size="sm"
                          variant="ghost"
                          iconLeft="pencil"
                          onClick={() => setModal({ editing: b })}
                        >
                          {t('admin-marketing.banner.edit')}
                        </Button>
                        {b.active ? (
                          <Button
                            size="sm"
                            variant="ghost"
                            iconLeft="power"
                            className="!text-red-600"
                            onClick={() =>
                              toggle.mutate(
                                { id: b.id, active: false },
                                {
                                  onSuccess: () =>
                                    toast.success(t('admin-marketing.banner.turned_off')),
                                  onError: (e) => toast.error(errorMessage(e, t)),
                                },
                              )
                            }
                          >
                            {t('admin-marketing.banner.off')}
                          </Button>
                        ) : (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() =>
                              toggle.mutate(
                                { id: b.id, active: true },
                                {
                                  onSuccess: () =>
                                    toast.success(t('admin-marketing.banner.turned_on')),
                                  onError: (e) => toast.error(errorMessage(e, t)),
                                },
                              )
                            }
                          >
                            {t('admin-marketing.banner.on')}
                          </Button>
                        )}
                        {canDeleteBanner(b) && (
                          <Button
                            size="sm"
                            variant="ghost"
                            iconLeft="trash-2"
                            className="!text-red-600"
                            onClick={() => setDeleting(b)}
                          >
                            {t('admin-marketing.banner.delete')}
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
      <BannerModal
        open={!!modal}
        editing={modal?.editing ?? null}
        rows={rows}
        onClose={() => setModal(null)}
      />
      <ConfirmDialog
        open={!!deleting}
        tone="danger"
        title={t('admin-marketing.banner.delete_title')}
        message={t('admin-marketing.banner.delete_desc', { title: deleting?.title ?? '' })}
        confirmLabel={t('admin-marketing.banner.delete_confirm')}
        loading={del.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() =>
          deleting &&
          del.mutate(deleting.id, {
            onSuccess: () => {
              toast.success(t('admin-marketing.banner.deleted'));
              setDeleting(null);
            },
            onError: (e) => toast.error(errorMessage(e, t)),
          })
        }
      />
    </div>
  );
}

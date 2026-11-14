// Sản phẩm trong danh mục con (prototype Catalog.jsx ProductList; "Xem SP") — route
// /catalog/sub/:id/products: tab Tất cả / Chờ duyệt / Đang bán / Đã ẩn, Duyệt / Từ chối (lý do),
// Khóa sản phẩm vi phạm (lý do) / Mở khóa.
import { useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatNumber, formatVND } from '@/shared/lib/format';
import {
  Badge,
  Button,
  Icon,
  Modal,
  Select,
  Skeleton,
  Tabs,
  toast,
  type BadgeVariant,
} from '@/shared/ui';
import { useCatalogProducts, useProductCommand } from '../application/useCatalog';
import {
  CATALOG_PRODUCT_TABS,
  LOCK_REASONS,
  productActions,
  type LockReason,
} from '../domain/catalog.rules';
import type {
  CatalogProduct,
  CatalogProductStatus,
  CatalogProductTab,
} from '../domain/catalog.types';

const th = 'px-4 py-2.5 whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-4 py-3 whitespace-nowrap';

const STATUS: Record<CatalogProductStatus | 'LOCKED', BadgeVariant> = {
  PUBLISHED: 'success',
  PENDING_APPROVAL: 'warning',
  REJECTED: 'danger',
  HIDDEN: 'danger',
  LOCKED: 'danger',
};

function ReasonModal({
  target,
  onClose,
}: {
  target: { p: CatalogProduct; kind: 'reject' | 'lock' } | null;
  onClose: () => void;
}) {
  const { t } = useT();
  const cmd = useProductCommand();
  const [reason, setReason] = useState<LockReason>('POLICY_VIOLATION');
  const kind = target?.kind ?? 'lock';
  return (
    <Modal
      open={!!target}
      onClose={onClose}
      title={t(`admin-catalog.products.${kind}_title`)}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant="danger"
            iconLeft={kind === 'lock' ? 'lock' : undefined}
            loading={cmd.isPending}
            onClick={() =>
              target &&
              cmd.mutate(
                {
                  kind,
                  id: target.p.id,
                  reason,
                  label: t(`admin-moderation.product.reasons.${reason}`),
                },
                {
                  onSuccess: () => {
                    toast.success(t(`admin-catalog.products.done.${kind}`));
                    onClose();
                  },
                  onError: (e) => toast.error(errorMessage(e, t)),
                },
              )
            }
          >
            {t(`admin-catalog.products.${kind}_confirm`)}
          </Button>
        </>
      }
    >
      <Select<LockReason>
        label={t(`admin-catalog.products.${kind}_reason`)}
        value={reason}
        onChange={setReason}
        options={LOCK_REASONS.map((r) => ({
          value: r,
          label: t(`admin-moderation.product.reasons.${r}`),
        }))}
      />
      {kind === 'lock' && (
        <p className="m-0 mt-2.5 text-xs text-muted leading-normal">
          {t('admin-catalog.products.lock_hint')}
        </p>
      )}
    </Modal>
  );
}

export default function CategoryProducts() {
  const { t, locale } = useT();
  const { id } = useParams();
  const [params] = useSearchParams();
  const subId = Number(id);
  const name = params.get('name') ?? `#${subId}`;
  const [tab, setTab] = useState<CatalogProductTab>('ALL');
  const q = useCatalogProducts(subId, tab);
  const cmd = useProductCommand();
  const [target, setTarget] = useState<{ p: CatalogProduct; kind: 'reject' | 'lock' } | null>(null);
  const run = (c: { kind: 'approve' | 'unlock'; id: number }) =>
    cmd.mutate(c, {
      onSuccess: () => toast.success(t(`admin-catalog.products.done.${c.kind}`)),
      onError: (e) => toast.error(errorMessage(e, t)),
    });
  return (
    <div className="max-w-[1180px]">
      <Link
        to="/catalog"
        className="inline-flex items-center gap-1.5 text-sm text-muted no-underline hover:underline mb-3"
      >
        <Icon name="chevron-left" size={16} />
        {t('admin-catalog.back')}
      </Link>
      <h2 className="m-0 mb-3.5 text-xl font-bold text-strong">
        {t('admin-catalog.products.title', { name })}
      </h2>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-hidden">
        <div className="px-4 pt-1 overflow-x-auto">
          <Tabs
            value={tab}
            onChange={setTab}
            aria-label={t('admin-catalog.products.tabs_label')}
            items={CATALOG_PRODUCT_TABS.map((x) => ({
              id: x,
              label: t(`admin-catalog.products.tabs.${x}`),
              badge: x === 'PENDING' && q.data?.pendingCount ? q.data.pendingCount : undefined,
            }))}
          />
        </div>
        <div className="overflow-x-auto">
          {!q.data ? (
            <Skeleton lines={4} className="m-5" />
          ) : (
            <table className="w-full min-w-[720px] border-collapse tabular-nums">
              <thead>
                <tr>
                  <th className={th}>{t('admin-catalog.products.col.product')}</th>
                  <th className={th}>{t('admin-catalog.products.col.shop')}</th>
                  <th className={cn(th, 'text-right')}>{t('admin-catalog.products.col.price')}</th>
                  <th className={cn(th, 'text-right')}>{t('admin-catalog.products.col.stock')}</th>
                  <th className={th}>{t('admin-catalog.products.col.status')}</th>
                  <th className={cn(th, 'text-right')}>{t('admin-catalog.products.col.action')}</th>
                </tr>
              </thead>
              <tbody>
                {q.data.items.length === 0 && (
                  <tr>
                    <td colSpan={6} className="p-9 text-center text-base text-muted">
                      {t('admin-catalog.products.empty')}
                    </td>
                  </tr>
                )}
                {q.data.items.map((p) => {
                  const acts = productActions(p);
                  const st = p.locked ? 'LOCKED' : p.status;
                  return (
                    <tr
                      key={p.id}
                      className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                    >
                      <td className={td}>
                        <div className="flex items-center gap-2.5">
                          <img
                            src={p.imageUrl}
                            alt=""
                            width={40}
                            height={40}
                            className="w-10 h-10 rounded-md object-cover bg-gray-100"
                          />
                          <span className="font-medium text-strong">{p.name}</span>
                        </div>
                      </td>
                      <td className={cn(td, 'text-muted')}>{p.shopName}</td>
                      <td className={cn(td, 'text-right font-semibold text-price')}>
                        {formatVND(p.price, { locale })}
                      </td>
                      <td className={cn(td, 'text-right')}>{formatNumber(p.stock, { locale })}</td>
                      <td className={td}>
                        <Badge variant={STATUS[st]}>
                          {t(`admin-catalog.products.status.${st}`)}
                        </Badge>
                        {p.reason && (st === 'REJECTED' || st === 'LOCKED' || st === 'HIDDEN') && (
                          <div className="text-xs text-subtle mt-[3px]">{p.reason}</div>
                        )}
                      </td>
                      <td className={cn(td, 'text-right')}>
                        <div className="flex gap-1 justify-end">
                          {acts.includes('reject') && (
                            <Button
                              size="sm"
                              variant="ghost"
                              className="!text-red-600"
                              onClick={() => setTarget({ p, kind: 'reject' })}
                            >
                              {t('admin-catalog.products.reject')}
                            </Button>
                          )}
                          {acts.includes('approve') && (
                            <Button
                              size="sm"
                              iconLeft="check"
                              loading={cmd.isPending && cmd.variables?.id === p.id}
                              onClick={() => run({ kind: 'approve', id: p.id })}
                            >
                              {t('admin-catalog.products.approve')}
                            </Button>
                          )}
                          {acts.includes('lock') && (
                            <Button
                              size="sm"
                              variant="ghost"
                              iconLeft="lock"
                              className="!text-red-600"
                              onClick={() => setTarget({ p, kind: 'lock' })}
                            >
                              {t('admin-catalog.products.lock')}
                            </Button>
                          )}
                          {acts.includes('unlock') && (
                            <Button
                              size="sm"
                              variant="ghost"
                              loading={cmd.isPending && cmd.variables?.id === p.id}
                              onClick={() => run({ kind: 'unlock', id: p.id })}
                            >
                              {t('admin-catalog.products.unlock')}
                            </Button>
                          )}
                          {acts.length === 0 && <span className="text-xs text-subtle">—</span>}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
      <ReasonModal target={target} onClose={() => setTarget(null)} />
    </div>
  );
}

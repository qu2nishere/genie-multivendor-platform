// Danh mục toàn sàn (prototype admin/Catalog.jsx) — plan G12.21, G12.24: 4 chỉ số, cây danh mục gốc
// (?id=), chi tiết (Hiển thị, Bộ thuộc tính, Sửa; bảng danh mục con: Sản phẩm / Hoa hồng / Xem SP),
// "Đề xuất danh mục từ người bán" (Duyệt / Từ chối có lý do), Thêm danh mục.
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatNumber } from '@/shared/lib/format';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Badge,
  Button,
  buttonClass,
  EmptyState,
  Icon,
  Input,
  Modal,
  Skeleton,
  Switch,
  toast,
  toIconName,
} from '@/shared/ui';
import { Kpi } from '@/shared/ui/dash';
import {
  useApproveProposal,
  useCategoryTree,
  useProposals,
  useRejectProposal,
  useUpdateCategory,
} from '../application/useCatalog';
import { proposalRejectSchema, type ProposalRejectValues } from '../domain/catalog.schema';
import type { CategoryProposal, RootCategory } from '../domain/catalog.types';
import { CatModal } from './CatModal';

const sheet = 'bg-surface-card rounded-lg shadow-sm overflow-hidden';
const th = 'px-4 py-2.5 whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-4 py-3 whitespace-nowrap';

function RejectProposalModal({ p, onClose }: { p: CategoryProposal | null; onClose: () => void }) {
  const { t } = useT();
  const reject = useRejectProposal();
  const form = useForm<ProposalRejectValues>({
    resolver: zodResolver(proposalRejectSchema),
    defaultValues: { reason: '' },
  });
  const reason = form.watch('reason');
  const submit = form.handleSubmit((v) => {
    if (!p) return;
    reject.mutate(
      { id: p.id, reason: v.reason },
      {
        onSuccess: () => {
          toast.success(t('admin-catalog.proposals.rejected'));
          form.reset({ reason: '' });
          onClose();
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  });
  return (
    <Modal
      open={!!p}
      onClose={onClose}
      title={t('admin-catalog.proposals.reject_title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant="danger"
            disabled={!reason.trim()}
            loading={reject.isPending}
            onClick={() => void submit()}
          >
            {t('admin-catalog.proposals.reject')}
          </Button>
        </>
      }
    >
      <form onSubmit={(e) => void submit(e)} noValidate>
        <p className="m-0 mb-3 text-base text-body">
          {t('admin-catalog.proposals.reject_desc', {
            name: p?.name ?? '',
            shop: p?.shop.name ?? '',
          })}
        </p>
        <Input
          label={t('admin-catalog.proposals.reason')}
          placeholder={t('admin-catalog.proposals.reason_placeholder')}
          required
          maxLength={500}
          {...form.register('reason')}
        />
      </form>
    </Modal>
  );
}

function Proposals() {
  const { t, locale } = useT();
  const q = useProposals();
  const approve = useApproveProposal();
  const [rejecting, setRejecting] = useState<CategoryProposal | null>(null);
  const items = q.data ?? [];
  return (
    <section className={sheet} aria-labelledby="cat-proposals">
      <div className="flex items-center gap-2 px-5 py-3.5 border-0 border-b border-solid border-line-subtle">
        <Icon name="clock" size={18} className="text-amber-600" />
        <h2 id="cat-proposals" className="m-0 text-base font-bold text-strong">
          {t('admin-catalog.proposals.title')}
        </h2>
        <Badge variant="flash" className="ml-1.5">
          {items.length}
        </Badge>
      </div>
      {q.isPending ? (
        <Skeleton lines={2} className="m-5" />
      ) : items.length === 0 ? (
        <div className="p-5 text-center text-sm text-subtle">
          {t('admin-catalog.proposals.empty')}
        </div>
      ) : (
        <ul className="list-none m-0 p-0">
          {items.map((p) => (
            <li
              key={p.id}
              className="flex items-center gap-3 px-5 py-[13px] border-0 border-t border-solid border-line-subtle first:border-t-0 flex-wrap"
            >
              <div className="flex-1 min-w-[200px]">
                <div className="text-sm font-semibold text-strong">{p.name}</div>
                <div className="text-xs text-muted">
                  {t('admin-catalog.proposals.meta', {
                    shop: p.shop.name,
                    date: formatDate(p.createdAt, { locale }),
                    parent: p.parent?.name ?? t('admin-catalog.proposals.new_root'),
                  })}
                </div>
                {p.reason && <div className="text-xs text-subtle mt-0.5">“{p.reason}”</div>}
              </div>
              <Button size="sm" variant="ghost" onClick={() => setRejecting(p)}>
                {t('admin-catalog.proposals.reject')}
              </Button>
              <Button
                size="sm"
                iconLeft="check"
                loading={approve.isPending && approve.variables?.id === p.id}
                onClick={() =>
                  approve.mutate(
                    { id: p.id, commissionRate: null },
                    {
                      onSuccess: () =>
                        toast.success(t('admin-catalog.proposals.approved', { name: p.name })),
                      onError: (e) => toast.error(errorMessage(e, t)),
                    },
                  )
                }
              >
                {t('admin-catalog.proposals.approve')}
              </Button>
            </li>
          ))}
        </ul>
      )}
      <RejectProposalModal p={rejecting} onClose={() => setRejecting(null)} />
    </section>
  );
}

function Detail({ c, onEdit }: { c: RootCategory; onEdit: () => void }) {
  const { t, locale } = useT();
  const update = useUpdateCategory();
  return (
    <section className={sheet} aria-labelledby="cat-detail">
      <div className="flex items-center gap-3 px-5 py-4 border-0 border-b border-solid border-line-subtle flex-wrap">
        <span className="w-10 h-10 rounded-md bg-blue-50 text-blue-600 grid place-items-center flex-none">
          <Icon name={toIconName(c.icon, 'package')} size={20} />
        </span>
        <div className="flex-1 min-w-[160px]">
          <h2 id="cat-detail" className="m-0 text-lg font-bold text-strong">
            {c.name}
          </h2>
          <div className="text-xs text-muted">
            {t('admin-catalog.detail.meta', {
              products: formatNumber(c.productCount, { locale }),
              shops: formatNumber(c.shopCount, { locale }),
              rate: formatNumber(c.commissionRate, { locale }),
            })}
          </div>
        </div>
        <div className="flex items-center gap-2 flex-wrap sm:flex-none">
          <label className="flex items-center gap-2 text-xs text-muted">
            {t('admin-catalog.detail.visible')}
            <Switch
              checked={c.active}
              disabled={update.isPending}
              onChange={() =>
                update.mutate(
                  { id: c.id, patch: { active: !c.active } },
                  {
                    onSuccess: () =>
                      toast.success(
                        t(c.active ? 'admin-catalog.detail.hidden' : 'admin-catalog.detail.shown'),
                      ),
                    onError: (e) => toast.error(errorMessage(e, t)),
                  },
                )
              }
            />
          </label>
          <Link
            to={`/catalog/${c.id}/attributes`}
            className={buttonClass({ size: 'sm', variant: 'outline' })}
          >
            <Icon name="sliders-horizontal" size={16} />
            {t('admin-catalog.detail.attributes')}
          </Link>
          <Button size="sm" variant="outline" iconLeft="pencil" onClick={onEdit}>
            {t('admin-catalog.detail.edit')}
          </Button>
        </div>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[560px] border-collapse tabular-nums">
          <thead>
            <tr>
              <th className={th}>{t('admin-catalog.detail.sub')}</th>
              <th className={cn(th, 'text-right')}>{t('admin-catalog.detail.products')}</th>
              <th className={cn(th, 'text-right')}>{t('admin-catalog.detail.rate')}</th>
              <th className={cn(th, 'text-right')}>{t('admin-catalog.detail.action')}</th>
            </tr>
          </thead>
          <tbody>
            {c.children.length === 0 && (
              <tr>
                <td colSpan={4} className="p-6 text-center text-sm text-muted">
                  {t('admin-catalog.detail.no_sub')}
                </td>
              </tr>
            )}
            {c.children.map((s) => {
              const to = `/catalog/sub/${s.id}/products?name=${encodeURIComponent(s.name)}`;
              return (
                <tr
                  key={s.id}
                  className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                >
                  <td className={td}>
                    <Link
                      to={to}
                      className="inline-flex items-center gap-2 font-semibold text-blue-600 no-underline hover:underline"
                    >
                      <Icon name="chevron-right" size={15} className="text-subtle" />
                      {s.name}
                    </Link>
                    {s.children.length > 0 && (
                      <div className="text-xs text-muted ml-[23px] whitespace-normal">
                        {s.children.map((l) => l.name).join(' · ')}
                      </div>
                    )}
                  </td>
                  <td className={cn(td, 'text-right')}>
                    {formatNumber(s.productCount, { locale })}
                  </td>
                  <td className={cn(td, 'text-right')}>
                    <span className="text-xs font-semibold text-brand-600 bg-brand-50 rounded-pill px-2.5 py-0.5">
                      {formatNumber(s.commissionRate, { locale })}%
                    </span>
                  </td>
                  <td className={cn(td, 'text-right')}>
                    <Link to={to} className={buttonClass({ size: 'sm', variant: 'ghost' })}>
                      {t('admin-catalog.detail.view_products')}
                    </Link>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export default function CatalogPage() {
  const { t, locale } = useT();
  const q = useCategoryTree();
  const [params, setParams] = useSearchParams();
  const [modal, setModal] = useState<{ editing: RootCategory | null } | null>(null);
  if (q.isPending) return <Skeleton lines={10} />;
  if (q.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(q.error, t)}
      />
    );
  const { roots, summary } = q.data;
  const selId = Number(params.get('id')) || roots[0]?.id;
  const current = roots.find((r) => r.id === selId) ?? roots[0];
  const select = (id: number) => setParams({ id: String(id) }, { replace: true });

  return (
    <div className="max-w-[1180px]">
      <div className="grid grid-cols-4 gap-4 mb-[18px] max-lg:grid-cols-2">
        <Kpi
          icon="boxes"
          tone={{ bg: 'var(--blue-50)', fg: 'var(--blue-600)' }}
          label={t('admin-catalog.kpi.roots')}
          value={formatNumber(summary.rootCount, { locale })}
        />
        <Kpi
          icon="package"
          tone={{ bg: 'var(--brand-50)', fg: 'var(--brand-600)' }}
          label={t('admin-catalog.kpi.children')}
          value={formatNumber(summary.childCount, { locale })}
        />
        <Kpi
          icon="package"
          tone={{ bg: 'var(--mint-50)', fg: 'var(--mint-600)' }}
          label={t('admin-catalog.kpi.products')}
          value={formatNumber(summary.productCount, { locale })}
        />
        <Kpi
          icon="clock"
          tone={{ bg: 'var(--amber-50)', fg: 'var(--amber-600)' }}
          label={t('admin-catalog.kpi.pending')}
          value={formatNumber(summary.pendingProposals, { locale })}
        />
      </div>

      <div className="grid grid-cols-[320px_minmax(0,1fr)] gap-[18px] items-start max-lg:grid-cols-1">
        <section className={sheet} aria-labelledby="cat-tree">
          <div className="flex items-center px-4 py-3.5 border-0 border-b border-solid border-line-subtle">
            <h2 id="cat-tree" className="m-0 text-sm font-bold text-strong">
              {t('admin-catalog.tree.title')}
            </h2>
            <Button
              size="sm"
              iconLeft="plus"
              className="ml-auto"
              onClick={() => setModal({ editing: null })}
            >
              {t('admin-catalog.tree.add')}
            </Button>
          </div>
          <ul className="list-none m-0 p-2">
            {roots.map((r) => {
              const on = r.id === current?.id;
              return (
                <li key={r.id}>
                  <button
                    type="button"
                    aria-current={on ? 'true' : undefined}
                    onClick={() => select(r.id)}
                    className={cn(
                      'flex items-center gap-[11px] w-full border-0 rounded-md px-3 py-2.5 cursor-pointer text-left focus-visible:outline-none focus-visible:shadow-focus',
                      on ? 'bg-blue-50' : 'bg-transparent hover:bg-gray-50',
                    )}
                  >
                    <span
                      className={cn(
                        'w-8 h-8 rounded-md grid place-items-center flex-none',
                        on ? 'bg-blue-500 text-white' : 'bg-surface-sunken text-muted',
                      )}
                    >
                      <Icon name={toIconName(r.icon, 'package')} size={17} />
                    </span>
                    <span className="flex-1 min-w-0">
                      <span
                        className={cn(
                          'block text-sm',
                          on ? 'font-semibold' : 'font-medium',
                          r.active ? 'text-strong' : 'text-subtle',
                        )}
                      >
                        {r.name}
                      </span>
                      <span className="block text-xs text-muted">
                        {t('admin-catalog.tree.meta', {
                          subs: r.children.length,
                          products: formatNumber(r.productCount, { locale }),
                        })}
                      </span>
                    </span>
                    {!r.active && <Badge variant="outline">{t('admin-catalog.tree.hidden')}</Badge>}
                    <Icon name="chevron-right" size={16} className="text-subtle flex-none" />
                  </button>
                </li>
              );
            })}
          </ul>
        </section>

        <div className="flex flex-col gap-[18px] min-w-0">
          {current && <Detail c={current} onEdit={() => setModal({ editing: current })} />}
          <Proposals />
        </div>
      </div>

      <CatModal
        open={!!modal}
        editing={modal?.editing ?? null}
        roots={roots}
        onClose={() => setModal(null)}
        onDeleted={() => setParams({}, { replace: true })}
      />
    </div>
  );
}

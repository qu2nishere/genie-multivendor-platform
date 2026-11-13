// Danh mục toàn sàn (prototype VendorCatalog.jsx) — plan G11.31: cây 2 cấp có số sản phẩm + hoa hồng (chỉ
// xem), "Đề xuất của Shop" + trạng thái, hộp "Đề xuất danh mục mới" (react-hook-form + zod; trùng tên →
// GNE-CAT-4091, tối đa 5 đề xuất chờ duyệt → GNE-CAT-4221).
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatNumber } from '@/shared/lib/format';
import { zodResolver } from '@/shared/lib/zodResolver';
import { Badge, Button, Icon, Input, Modal, Select, Skeleton, toast } from '@/shared/ui';
import { useCatalog, useCreateProposal, useProposals } from '../application/useSellerProducts';
import { proposalSchema, type ProposalValues } from '../domain/product.schema';
import type { ProposalStatus } from '../domain/product.types';

const STATE: Record<ProposalStatus, 'warning' | 'success' | 'danger'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};
const card = 'bg-surface-card rounded-lg shadow-sm px-5 py-[18px]';

export default function VendorCatalogPage() {
  const { t, locale } = useT();
  const tree = useCatalog();
  const props = useProposals();
  const create = useCreateProposal();
  const [open, setOpen] = useState<Set<number>>(() => new Set([1]));
  const [modal, setModal] = useState(false);
  const form = useForm<ProposalValues>({
    resolver: zodResolver(proposalSchema),
    defaultValues: { name: '', parentId: null, reason: '' },
  });
  const { errors } = form.formState;
  const parentId = form.watch('parentId');
  const name = form.watch('name');
  const toggle = (id: number) =>
    setOpen((s) => {
      const x = new Set(s);
      if (x.has(id)) x.delete(id);
      else x.add(id);
      return x;
    });

  const submit = form.handleSubmit((v) =>
    create.mutate(v, {
      onSuccess: () => {
        toast.success(t('seller-products.catalog.sent'));
        setModal(false);
      },
      onError: (e) => {
        if (isApiError(e) && e.code === 'GNE-CAT-4091') {
          const ex = e.details.existing_category;
          form.setError('name', {
            message:
              typeof ex === 'string'
                ? t('seller-products.catalog.err.dup', { name: ex })
                : errorMessage(e, t),
          });
        } else form.setError('root', { message: errorMessage(e, t) });
      },
    }),
  );

  return (
    <div className="max-w-[1180px] grid grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)] gap-4 items-start max-lg:grid-cols-1">
      <section className={card} aria-labelledby="cat-tree">
        <h2 id="cat-tree" className="m-0 mb-1 font-bold text-base text-strong">
          {t('seller-products.catalog.tree')}
        </h2>
        <p className="m-0 mb-3 text-xs text-muted">{t('seller-products.catalog.tree_hint')}</p>
        {!tree.data ? (
          <Skeleton lines={6} />
        ) : (
          <ul className="m-0 p-0 list-none">
            {tree.data.map((c) => {
              const on = open.has(c.id);
              return (
                <li key={c.id} className="border-0 border-t border-solid border-line-subtle">
                  <button
                    type="button"
                    aria-expanded={on}
                    onClick={() => toggle(c.id)}
                    className="flex items-center gap-2.5 w-full px-1 py-[11px] border-0 bg-transparent cursor-pointer text-left flex-wrap"
                  >
                    <Icon
                      name={on ? 'chevron-down' : 'chevron-right'}
                      size={16}
                      className="text-subtle"
                    />
                    <span className="flex-1 font-semibold text-sm text-strong">{c.name}</span>
                    <span className="text-xs text-muted">
                      {t('seller-products.catalog.count', {
                        count: formatNumber(c.productCount, { locale }),
                      })}
                    </span>
                    <Badge variant="neutral">
                      {t('seller-products.catalog.rate', {
                        rate: formatNumber(c.commissionRate, { locale }),
                      })}
                    </Badge>
                  </button>
                  {on && (
                    <ul className="m-0 p-0 list-none">
                      {c.children.map((s) => (
                        <li key={s.id} className="flex items-center gap-2.5 py-2 pr-1 pl-[30px]">
                          <span className="flex-1 text-sm text-body">{s.name}</span>
                          <span className="text-xs text-muted">
                            {t('seller-products.catalog.count', {
                              count: formatNumber(s.productCount, { locale }),
                            })}
                          </span>
                          <span className="text-xs text-muted w-[92px] text-right">
                            {t('seller-products.catalog.rate', {
                              rate: formatNumber(s.commissionRate, { locale }),
                            })}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <section className={card} aria-labelledby="cat-props">
        <div className="flex items-center mb-3 gap-2 flex-wrap">
          <h2 id="cat-props" className="m-0 font-bold text-base text-strong">
            {t('seller-products.catalog.proposals')}
          </h2>
          <Button
            size="sm"
            iconLeft="plus"
            className="ml-auto"
            onClick={() => {
              form.reset({ name: '', parentId: null, reason: '' });
              setModal(true);
            }}
          >
            {t('seller-products.catalog.propose')}
          </Button>
        </div>
        {!props.data ? (
          <Skeleton lines={3} />
        ) : props.data.length === 0 ? (
          <p className="m-0 py-3 text-sm text-muted">{t('seller-products.catalog.none')}</p>
        ) : (
          <ul className="m-0 p-0 list-none">
            {props.data.map((p) => (
              <li
                key={p.id}
                className="flex items-center gap-2.5 py-[11px] border-0 border-t border-solid border-line-subtle"
              >
                <div className="flex-1 min-w-0">
                  <div className="font-semibold text-sm text-strong">{p.name}</div>
                  <div className="text-xs text-muted">
                    {t('seller-products.catalog.under', {
                      parent: p.parent?.name ?? t('seller-products.catalog.root'),
                      date: formatDate(p.createdAt, { locale }),
                    })}
                    {p.rejectReason && <span className="block text-red-600">{p.rejectReason}</span>}
                  </div>
                </div>
                <Badge variant={STATE[p.status]}>
                  {t(`seller-products.catalog.state.${p.status}`)}
                </Badge>
              </li>
            ))}
          </ul>
        )}
      </section>

      <Modal
        open={modal}
        onClose={() => setModal(false)}
        title={t('seller-products.catalog.propose')}
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              iconLeft="send"
              loading={create.isPending}
              disabled={!name.trim()}
              onClick={() => void submit()}
            >
              {t('seller-products.catalog.send')}
            </Button>
          </>
        }
      >
        <form className="flex flex-col gap-3.5" onSubmit={(e) => void submit(e)} noValidate>
          <Input
            label={t('seller-products.catalog.name')}
            required
            placeholder={t('seller-products.catalog.name_placeholder')}
            error={errors.name?.message ? t(errors.name.message) : undefined}
            {...form.register('name')}
          />
          <Select
            label={t('seller-products.catalog.parent')}
            value={parentId === null ? 'ROOT' : String(parentId)}
            onChange={(v) => form.setValue('parentId', v === 'ROOT' ? null : Number(v))}
            options={[
              { value: 'ROOT', label: t('seller-products.catalog.new_root') },
              ...(tree.data ?? []).map((c) => ({ value: String(c.id), label: c.name })),
            ]}
          />
          <Input
            label={t('seller-products.catalog.reason')}
            placeholder={t('seller-products.catalog.reason_placeholder')}
            error={errors.reason?.message ? t(errors.reason.message) : undefined}
            {...form.register('reason')}
          />
          {errors.root?.message && (
            <p className={cn('m-0 flex items-center gap-1.5 text-xs text-red-600')} role="alert">
              <Icon name="circle-alert" size={13} />
              {errors.root.message}
            </p>
          )}
        </form>
      </Modal>
    </div>
  );
}

// Sản phẩm của shop (prototype Products.jsx Products) — plan G11.22: tab trạng thái có số đếm (?tab=), tìm
// theo tên, bảng Sản phẩm / SKU / Giá / Kho / Đã bán / Trạng thái, Sửa / Xem / Ẩn / Hiện / Xóa (xác nhận).
import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatCompact, formatNumber, formatVND } from '@/shared/lib/format';
import {
  Badge,
  Button,
  buttonClass,
  ConfirmDialog,
  EmptyState,
  Icon,
  Pagination,
  Skeleton,
  Tabs,
  toast,
} from '@/shared/ui';
import { useDeleteProduct, useProductList, useSetHidden } from '../application/useSellerProducts';
import { STATUS_BADGE } from '../domain/product.rules';
import type { ProductTab, SellerProductRow } from '../domain/product.types';

const TABS: ProductTab[] = ['ALL', 'PUBLISHED', 'DRAFT', 'PENDING_APPROVAL', 'REJECTED', 'HIDDEN'];
const isTab = (v: string | null): v is ProductTab => !!v && (TABS as string[]).includes(v);
const th = 'px-4 py-[11px] whitespace-nowrap';
const td = 'px-4 py-3 whitespace-nowrap';

export default function ProductsPage() {
  const { t, locale } = useT();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const tp = params.get('tab');
  const tab: ProductTab = isTab(tp) ? tp : 'ALL';
  const [q, setQ] = useState('');
  const [term, setTerm] = useState('');
  const [page, setPage] = useState(1);
  const [del, setDel] = useState<SellerProductRow | null>(null);
  useEffect(() => {
    const id = setTimeout(() => {
      setTerm(q.trim());
      setPage(1);
    }, 250);
    return () => clearTimeout(id);
  }, [q]);
  useEffect(() => setPage(1), [tab]);
  const list = useProductList({ tab, q: term, page });
  const hide = useSetHidden();
  const remove = useDeleteProduct();
  const data = list.data;
  const fail = (e: unknown) => toast.error(errorMessage(e, t));

  return (
    <div className="max-w-[1180px]">
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div className="flex-1 min-w-0">
          <Tabs<ProductTab>
            aria-label={t('seller-products.list.tabs')}
            value={tab}
            onChange={(id) => navigate(id === 'ALL' ? '/products' : `/products?tab=${id}`)}
            items={TABS.map((id) => ({
              id,
              label: t(`seller-products.list.tab.${id}`),
              badge: id === 'HIDDEN' ? data?.counts[id] || undefined : data?.counts[id],
            }))}
          />
        </div>
        <Link to="/products/new" className={buttonClass({})}>
          <Icon name="plus-circle" size={18} />
          {t('seller-products.list.add')}
        </Link>
      </div>
      <div className="relative max-w-[360px] mb-3.5">
        <Icon
          name="search"
          size={16}
          className="absolute left-3 top-3 text-subtle pointer-events-none"
        />
        <input
          type="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          aria-label={t('seller-products.list.search')}
          placeholder={t('seller-products.list.search')}
          className="w-full h-10 pl-9 pr-3 border-[1.5px] border-solid border-line rounded-md text-sm bg-surface-card text-body outline-none focus:border-primary"
        />
      </div>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-hidden">
        {!data ? (
          <Skeleton lines={7} className="m-4" />
        ) : data.items.length === 0 ? (
          <div className="py-12">
            <EmptyState
              icon="boxes"
              text={t('seller-products.list.empty')}
              action={
                <Link to="/products/new" className={buttonClass({ variant: 'outline' })}>
                  {t('seller-products.list.add')}
                </Link>
              }
            />
          </div>
        ) : (
          <div className="relative overflow-x-auto">
            <table className="w-full border-collapse tabular-nums min-w-[860px]">
              <thead>
                <tr className="text-xs font-semibold text-muted text-left bg-gray-25">
                  <th scope="col" className={th}>
                    {t('seller-products.list.col.product')}
                  </th>
                  <th scope="col" className={th}>
                    SKU
                  </th>
                  <th scope="col" className={`${th} text-right`}>
                    {t('seller-products.list.col.price')}
                  </th>
                  <th scope="col" className={`${th} text-right`}>
                    {t('seller-products.list.col.stock')}
                  </th>
                  <th scope="col" className={`${th} text-right`}>
                    {t('seller-products.list.col.sold')}
                  </th>
                  <th scope="col" className={th}>
                    {t('seller-products.list.col.status')}
                  </th>
                  <th scope="col" className={th}>
                    <span className="sr-only">{t('seller-products.list.col.actions')}</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((p) => {
                  const b = STATUS_BADGE[p.status];
                  const view = p.status === 'PENDING_APPROVAL';
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
                            width={44}
                            height={44}
                            className="w-11 h-11 rounded-md object-cover flex-none"
                          />
                          <Link
                            to={`/products/${p.id}/edit`}
                            className="font-medium text-strong max-w-[280px] truncate no-underline hover:underline"
                            title={p.name}
                          >
                            {p.name}
                          </Link>
                        </div>
                      </td>
                      <td className={`${td} text-muted`}>
                        {t('seller-products.list.skus', { count: p.variantCount })}
                      </td>
                      <td className={`${td} text-right font-semibold text-price`}>
                        {formatVND(p.priceMin, { locale })}
                      </td>
                      <td
                        className={`${td} text-right ${p.stockTotal === 0 ? 'text-red-600 font-semibold' : ''}`}
                      >
                        {p.stockTotal === 0
                          ? t('seller-products.list.out')
                          : formatNumber(p.stockTotal, { locale })}
                      </td>
                      <td className={`${td} text-right text-muted`}>
                        {p.soldCount ? formatCompact(p.soldCount, { locale }) : '—'}
                      </td>
                      <td className={td}>
                        <Badge variant={b.variant} icon={b.icon}>
                          {t(`seller-products.list.status.${p.status}`)}
                        </Badge>
                      </td>
                      <td className={`${td} text-right`}>
                        <div className="flex gap-1 justify-end">
                          <Link
                            to={`/products/${p.id}/edit`}
                            className={buttonClass({ variant: 'ghost', size: 'sm' })}
                          >
                            <Icon name={view ? 'eye' : 'pencil'} size={16} />
                            {t(view ? 'seller-products.list.view' : 'seller-products.list.edit')}
                          </Link>
                          {p.status === 'PUBLISHED' && (
                            <Button
                              variant="ghost"
                              size="sm"
                              iconLeft="eye-off"
                              onClick={() =>
                                hide.mutate(
                                  { id: p.id, hidden: true },
                                  {
                                    onSuccess: () =>
                                      toast.success(t('seller-products.list.hidden')),
                                    onError: fail,
                                  },
                                )
                              }
                            >
                              {t('seller-products.list.hide')}
                            </Button>
                          )}
                          {p.status === 'HIDDEN' && (
                            <Button
                              variant="ghost"
                              size="sm"
                              iconLeft="eye"
                              onClick={() =>
                                hide.mutate(
                                  { id: p.id, hidden: false },
                                  {
                                    onSuccess: () => toast.success(t('seller-products.list.shown')),
                                    onError: fail,
                                  },
                                )
                              }
                            >
                              {t('seller-products.list.show')}
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            size="sm"
                            iconLeft="trash-2"
                            className="!text-red-600"
                            onClick={() => setDel(p)}
                          >
                            {t('seller-products.list.delete')}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
      {data && (
        <Pagination page={page} total={data.totalPages} onChange={setPage} className="mt-4" />
      )}
      <ConfirmDialog
        open={!!del}
        tone="danger"
        loading={remove.isPending}
        title={t('seller-products.list.delete_title')}
        message={t('seller-products.list.delete_body', { name: del?.name ?? '' })}
        confirmLabel={t('seller-products.list.delete_ok')}
        onCancel={() => setDel(null)}
        onConfirm={() =>
          del &&
          remove.mutate(del.id, {
            onSuccess: () => {
              toast.success(t('seller-products.list.deleted'));
              setDel(null);
            },
            onError: (e) => {
              fail(e);
              setDel(null);
            },
          })
        }
      />
    </div>
  );
}

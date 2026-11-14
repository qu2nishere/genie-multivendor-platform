// "Sản phẩm yêu thích" (prototype Account.jsx WishlistPanel) — plan G10.58: danh sách theo store
// yêu thích dùng chung với nút tim, bỏ thích, thêm vào giỏ (phân loại đầu tiên còn hàng); shop đang tạm
// nghỉ thì khóa nút thêm vào giỏ.
import { useStore } from '@nanostores/react';
import { useEffect, useState } from 'react';
import { addToCart } from '@/features/cart';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import { Button, EmptyState, PanelHead, Skeleton, toast } from '@/shared/ui';
import { $wishlist, loadWishlist, toggleWishlist } from '../application/wishlist.store';
import { priceOf, productHref, stockOf, variantLabel } from '../domain/product.rules';
import type { ProductSummary } from '../domain/product.types';
import { loadProductDetail, useWishlistProducts } from '../application/useWishlistItems';

/** Thêm vào giỏ với tổ hợp phân loại đầu tiên còn hàng (prototype: phân loại đầu tiên). */
async function addDefault(p: ProductSummary) {
  const d = await loadProductDetail(p.id);
  if (!d) throw new Error('not_found');
  const sku = d.skus.find((s) => {
    const sel = Object.fromEntries(d.variantGroups.map((g, i) => [g.name, s.options[i]]));
    return stockOf(d.variantGroups, sel) > 0;
  });
  if (!sku) throw new Error('out_of_stock');
  const sel = Object.fromEntries(d.variantGroups.map((g, i) => [g.name, sku.options[i]]));
  return addToCart(
    {
      variantId: sku.id,
      productId: d.id,
      productSlug: d.slug,
      shopId: d.shopId,
      title: d.title,
      image: d.image,
      variantLabel: variantLabel(d.variantGroups, sel),
      price: priceOf(d, sel),
      originalPrice: d.originalPrice,
      qty: 1,
      stock: stockOf(d.variantGroups, sel),
    },
    true,
  );
}

function Panel() {
  const { t, locale } = useT();
  const ids = useStore($wishlist);
  const [busy, setBusy] = useState<number | null>(null);
  useEffect(() => void loadWishlist(), []);
  const q = useWishlistProducts([...ids]);
  const items = (q.data ?? []).filter((p) => ids.has(p.id));

  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm px-[22px] py-5 max-sm:px-4"
      aria-labelledby="wish-title"
    >
      <PanelHead
        id="wish-title"
        icon="heart"
        title={t('product.wishlist.title', { count: ids.size })}
        sub={t('product.wishlist.sub')}
      />
      {q.isPending && ids.size > 0 && <Skeleton lines={3} className="mt-4" />}
      {ids.size === 0 && <EmptyState icon="heart" text={t('product.wishlist.empty')} />}
      <ul className="list-none m-0 p-0 mt-3">
        {items.map((p) => (
          <li
            key={p.id}
            className="flex items-center gap-3 py-3 border-0 border-t border-solid border-line-subtle flex-wrap"
          >
            <a href={productHref(p)} className="flex-none">
              <img
                src={p.image}
                alt=""
                width={64}
                height={64}
                loading="lazy"
                className="w-16 h-16 rounded-sm object-cover"
              />
            </a>
            <div className="flex-1 min-w-[160px]">
              <a
                href={productHref(p)}
                className="block text-sm text-body truncate no-underline hover:underline"
              >
                {p.title}
              </a>
              <div className="font-semibold text-sm text-price mt-[3px] tabular-nums">
                {formatVND(p.price, { locale })}
              </div>
            </div>
            <Button
              size="sm"
              variant="ghost"
              iconLeft="heart"
              onClick={() =>
                void toggleWishlist(p.id).catch((e) => toast.error(errorMessage(e, t)))
              }
            >
              {t('product.wishlist.remove')}
            </Button>
            <Button
              size="sm"
              iconLeft="shopping-cart"
              disabled={p.shopOnVacation}
              loading={busy === p.id}
              onClick={() => {
                setBusy(p.id);
                void addDefault(p)
                  .then((r) => {
                    if (r.ok) toast.success(t('cart.add.added'));
                    else toast.error(errorMessage(r.error, t));
                  })
                  .catch(() => toast.error(t('product.wishlist.unavailable')))
                  .finally(() => setBusy(null));
              }}
            >
              {t('product.wishlist.add')}
            </Button>
          </li>
        ))}
      </ul>
    </section>
  );
}

export default function WishlistPanel() {
  return (
    <Providers>
      <Panel />
    </Providers>
  );
}

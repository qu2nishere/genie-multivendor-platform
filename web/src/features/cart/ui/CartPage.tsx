// Trang giỏ hàng nhiều shop (prototype Cart.jsx) — plan G10.04–G10.07, island client:load.
// Nhóm theo shop; chọn tất cả / theo shop / từng dòng; đổi số lượng (về 0 hỏi xóa, vượt tồn báo);
// đổi phân loại; xóa nhiều dòng; dòng hết hàng không chọn được. Khách xem giỏ tạm trong trình duyệt,
// bấm "Mua hàng" thì nhắc đăng nhập (giỏ tạm gộp vào tài khoản sau khi đăng nhập).
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { requireLogin } from '@/features/auth';
import { saveCheckoutDraft } from '@/features/checkout';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { Providers } from '@/shared/providers/Providers';
import { VoucherPicker } from '@/features/promotions';
import { formatNumber, formatVND } from '@/shared/lib/format';
import { Button, ConfirmDialog, Icon, Skeleton, Switch, toast } from '@/shared/ui';
import {
  changeGuestVariant,
  removeLines,
  setGuestLineQty,
  setLineQty,
  setLineVariant,
} from '../application/cart.store';
import { useCart } from '../application/useCart';
import {
  checkState,
  decideQty,
  groupByShop,
  initialSelection,
  isBuyable,
  lineKeyOf,
  pruneSelection,
  toggleGroup,
  toggleKey,
} from '../domain/cart.rules';
import type { CartLine } from '../domain/cart.types';
import { CartBar } from './CartBar';
import { CartGroupCard } from './CartGroupCard';
import { useCartPricing } from './useCartPricing';
import { VariantModal } from './VariantModal';

export interface CartPageProps {
  /** ?buy=<khóa dòng> từ nút "Mua ngay": chỉ chọn sẵn dòng đó. */
  buyKey: string | null;
}

type Confirm =
  { kind: 'remove'; lines: CartLine[]; zero?: boolean } | { kind: 'over_stock'; stock: number };

function CartScreen({ buyKey }: CartPageProps) {
  const { t, locale } = useT();
  const cart = useCart();
  const [selected, setSelected] = useState<Set<string> | null>(null);
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [variantLine, setVariantLine] = useState<CartLine | null>(null);
  const [busy, setBusy] = useState(false);
  /** Hộp voucher đang mở: sàn (platform) hoặc voucher của một shop. */
  const [picker, setPicker] = useState<
    { mode: 'platform' } | { mode: 'shop'; shopId: number } | null
  >(null);

  // Lựa chọn ban đầu khi giỏ nạp xong; sau đó chỉ bỏ khóa không còn trong giỏ
  useEffect(() => {
    if (cart.loading || cart.pending) return;
    setSelected((s) =>
      s === null ? initialSelection(cart.lines, buyKey) : pruneSelection(s, cart.lines),
    );
  }, [cart.lines, cart.loading, cart.pending, buyKey]);

  // Bỏ ?buy= khỏi URL để tải lại trang không chọn lại riêng dòng đó
  useEffect(() => {
    if (!buyKey) return;
    const u = new URL(window.location.href);
    u.searchParams.delete('buy');
    window.history.replaceState(null, '', u.pathname + u.search + u.hash);
  }, [buyKey]);

  const sel = useMemo(() => selected ?? new Set<string>(), [selected]);
  const groups = useMemo(() => groupByShop(cart.lines), [cart.lines]);
  const picked = useMemo(
    () => cart.lines.filter((l) => sel.has(l.key) && isBuyable(l)),
    [cart.lines, sel],
  );
  const buyableKeys = cart.lines.filter(isBuyable).map((l) => l.key);
  const pricing = useCartPricing(
    picked,
    groups.map((g) => g.shop.id),
  );
  const { price } = pricing;
  const vnd = (n: number) => formatVND(n, { locale });
  const vouchersOn = !cart.guest;

  const run = async (job: () => Promise<void> | void) => {
    setBusy(true);
    try {
      await job();
    } catch (e) {
      toast.error(errorMessage(e, t));
    } finally {
      setBusy(false);
    }
  };

  const remove = (lines: CartLine[]) =>
    run(async () => {
      if (cart.guest) lines.forEach((l) => setGuestLineQty(l.variantId, 0));
      else await removeLines(lines);
      setSelected((s) => {
        const n = new Set(s);
        lines.forEach((l) => n.delete(l.key));
        return n;
      });
    });

  const onQty = (l: CartLine, qty: number) => {
    const d = decideQty(l, qty);
    if (d.kind === 'confirm_remove') setConfirm({ kind: 'remove', lines: [l], zero: true });
    else if (d.kind === 'over_stock') setConfirm({ kind: 'over_stock', stock: d.stock });
    else
      void run(async () => {
        if (cart.guest) setGuestLineQty(l.variantId, d.qty);
        else await setLineQty(l, d.qty);
      });
  };

  const onVariant = (l: CartLine, next: { id: number; label: string; stock: number }) =>
    run(async () => {
      if (next.id !== l.variantId) {
        if (cart.guest) changeGuestVariant(l.variantId, next.id, l.qty, next.stock);
        else await setLineVariant(l, next.id);
        const key = lineKeyOf(l.productId, next.label);
        setSelected((s) => {
          const n = new Set(s);
          if (n.delete(l.key)) n.add(key);
          return n;
        });
      }
      setVariantLine(null);
    });

  const checkout = () => {
    if (cart.guest) {
      requireLogin('/cart');
      return;
    }
    saveCheckoutDraft({ itemIds: picked.map((l) => l.id) });
    window.location.assign('/checkout');
  };

  if (cart.pending || (cart.loading && cart.lines.length === 0) || selected === null) {
    return <CartSkeleton />;
  }

  if (cart.lines.length === 0) {
    return (
      <div className="max-w-[600px] mx-auto px-6 py-[90px] text-center">
        <Icon name="shopping-cart" size={56} className="text-subtle mx-auto mb-4" />
        <h1 className="m-0 mb-2 text-2xl font-semibold text-strong">
          {t('cart.page.empty_title')}
        </h1>
        <p className="m-0 mb-[22px] text-muted">{t('cart.page.empty_text')}</p>
        <Button href="/" iconLeft="house">
          {t('cart.page.continue')}
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-[1280px] px-6 pt-[18px] max-md:px-3">
      <h1 className="m-0 mb-4 text-3xl font-bold text-strong">
        {t('cart.page.title')}{' '}
        <span className="text-muted font-normal text-lg">
          {t('cart.page.count', { count: cart.lines.length })}
        </span>
      </h1>

      {cart.guest && (
        <GuestNotice>
          <Button size="sm" variant="secondary" onClick={() => requireLogin('/cart')}>
            {t('common.action.login')}
          </Button>
        </GuestNotice>
      )}

      {groups.map((g) => (
        <CartGroupCard
          key={g.shop.id}
          group={g}
          selected={sel}
          busy={busy}
          onToggleGroup={() =>
            setSelected((s) =>
              toggleGroup(
                s ?? new Set(),
                g.lines.filter(isBuyable).map((l) => l.key),
              ),
            )
          }
          onToggle={(l) => setSelected((s) => toggleKey(s ?? new Set(), l.key))}
          onQty={onQty}
          onVariant={setVariantLine}
          onRemove={(l) => setConfirm({ kind: 'remove', lines: [l] })}
          shipLabel={
            !pricing.ready
              ? undefined
              : g.lines.some((l) => sel.has(l.key)) && pricing.freeByShop(g.shop.id)
                ? t('cart.group.ship_free')
                : t('cart.group.ship_fee', { fee: vnd(pricing.shipOfShop(g.shop.id)) })
          }
          shopCut={price.shopByShop[g.shop.id] ?? 0}
          lineCuts={price.perLine}
          voucherSlot={
            vouchersOn && (
              <button
                type="button"
                className="tr-vfoot__voucher"
                onClick={() => setPicker({ mode: 'shop', shopId: g.shop.id })}
              >
                <Icon name="ticket" size={16} className="text-primary" />
                {t('cart.group.voucher')}
                {(price.shopByShop[g.shop.id] ?? 0) > 0 && (
                  <span className="text-primary">(−{vnd(price.shopByShop[g.shop.id])})</span>
                )}
              </button>
            )
          }
        />
      ))}

      <CartBar
        lineCount={cart.lines.length}
        selectedCount={picked.length}
        allState={checkState(sel, buyableKeys)}
        orderCount={new Set(picked.map((l) => l.shop.id)).size}
        total={price.total}
        shipping={price.shipping}
        discount={price.discountTotal}
        busy={busy || !pricing.ready}
        tools={
          vouchersOn && (
            <>
              <button
                type="button"
                className="tr-cart-tools__btn"
                onClick={() => setPicker({ mode: 'platform' })}
              >
                <Icon name="ticket" size={15} /> {t('cart.bar.voucher')}
                {price.platformDiscount > 0 && (
                  <b className="text-primary">(−{vnd(price.platformDiscount)})</b>
                )}
                {price.freeshipDiscount > 0 && (
                  <b className="text-mint-600">{t('cart.bar.freeship')}</b>
                )}
              </button>
              <label
                className={
                  'inline-flex items-center gap-1.5 text-link text-sm whitespace-nowrap' +
                  (pricing.pointsUsable > 0 ? '' : ' opacity-40')
                }
              >
                <Icon name="star" size={15} className="text-gold-500" />
                {t('cart.bar.points', { points: formatNumber(pricing.pointsUsable, { locale }) })}
                <Switch
                  checked={pricing.usePoints && pricing.pointsUsable > 0}
                  disabled={pricing.pointsUsable <= 0}
                  onChange={() => pricing.setUsePoints(!pricing.usePoints)}
                  aria-label={t('cart.bar.points_toggle')}
                />
              </label>
            </>
          )
        }
        onToggleAll={() =>
          setSelected(checkState(sel, buyableKeys) === 'all' ? new Set() : new Set(buyableKeys))
        }
        onRemoveSelected={() => setConfirm({ kind: 'remove', lines: picked })}
        onCheckout={checkout}
      />

      <ConfirmDialog
        open={confirm !== null}
        tone={confirm?.kind === 'remove' ? 'danger' : 'primary'}
        title={t(
          confirm?.kind === 'over_stock' ? 'cart.confirm.over_title' : 'cart.confirm.remove_title',
        )}
        message={
          confirm?.kind === 'over_stock'
            ? t('cart.confirm.over_text', { stock: confirm.stock })
            : confirm?.kind === 'remove' && confirm.lines.length > 1
              ? t('cart.confirm.remove_many', { count: confirm.lines.length })
              : t(
                  confirm?.kind === 'remove' && confirm.zero
                    ? 'cart.confirm.remove_zero'
                    : 'cart.confirm.remove_one',
                )
        }
        confirmLabel={t(confirm?.kind === 'remove' ? 'common.action.delete' : 'cart.confirm.ok')}
        cancelLabel={t('common.action.cancel')}
        onCancel={() => setConfirm(null)}
        onConfirm={() => {
          if (confirm?.kind === 'remove') void remove(confirm.lines);
          setConfirm(null);
        }}
      />

      {vouchersOn && (
        <VoucherPicker
          open={picker !== null}
          onClose={() => setPicker(null)}
          title={t(
            picker?.mode === 'shop' ? 'cart.picker.shop_title' : 'cart.picker.platform_title',
          )}
          mode={picker?.mode ?? 'platform'}
          shopId={picker?.mode === 'shop' ? picker.shopId : null}
          lines={
            picker?.mode === 'shop'
              ? pricing.lines.filter((l) => l.shopId === picker.shopId)
              : pricing.lines
          }
          selection={pricing.selection}
          ctx={pricing.ctx}
          onFound={pricing.addVoucher}
          onConfirm={pricing.setSelection}
        />
      )}

      <VariantModal
        line={variantLine}
        busy={busy}
        onClose={() => setVariantLine(null)}
        onConfirm={(l, next) => void onVariant(l, next)}
      />
    </div>
  );
}

function GuestNotice({ children }: { children: ReactNode }) {
  const { t } = useT();
  return (
    <div className="flex items-center gap-3 flex-wrap mb-4 px-4 py-3 rounded-lg bg-surface-card shadow-sm text-sm text-body">
      <Icon name="info" size={18} className="text-primary flex-none" />
      <span className="flex-1 min-w-[200px]">{t('cart.page.guest_notice')}</span>
      {children}
    </div>
  );
}

function CartSkeleton() {
  const { t } = useT();
  return (
    <div className="mx-auto max-w-[1280px] px-6 pt-[18px] max-md:px-3" aria-busy="true">
      <h1 className="m-0 mb-4 text-3xl font-bold text-strong">{t('cart.page.title')}</h1>
      {[0, 1].map((i) => (
        <div key={i} className="bg-surface-card rounded-lg shadow-sm mb-4 p-[18px] grid gap-4">
          <Skeleton className="h-6 w-56" />
          <Skeleton className="h-[72px]" />
          <Skeleton className="h-[72px]" />
        </div>
      ))}
    </div>
  );
}

export default function CartPage(props: CartPageProps) {
  return (
    <Providers>
      <CartScreen {...props} />
    </Providers>
  );
}

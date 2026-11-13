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
import { Button, ConfirmDialog, Icon, Skeleton, toast } from '@/shared/ui';
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
  linesSubtotal,
  pruneSelection,
  toggleGroup,
  toggleKey,
} from '../domain/cart.rules';
import type { CartLine } from '../domain/cart.types';
import { CartBar } from './CartBar';
import { CartGroupCard } from './CartGroupCard';
import { VariantModal } from './VariantModal';

export interface CartPageProps {
  /** ?buy=<khóa dòng> từ nút "Mua ngay": chỉ chọn sẵn dòng đó. */
  buyKey: string | null;
}

type Confirm =
  { kind: 'remove'; lines: CartLine[]; zero?: boolean } | { kind: 'over_stock'; stock: number };

function CartScreen({ buyKey }: CartPageProps) {
  const { t } = useT();
  const cart = useCart();
  const [selected, setSelected] = useState<Set<string> | null>(null);
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [variantLine, setVariantLine] = useState<CartLine | null>(null);
  const [busy, setBusy] = useState(false);

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
  const picked = cart.lines.filter((l) => sel.has(l.key) && isBuyable(l));
  const buyableKeys = cart.lines.filter(isBuyable).map((l) => l.key);

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
        />
      ))}

      <CartBar
        lineCount={cart.lines.length}
        selectedCount={picked.length}
        allState={checkState(sel, buyableKeys)}
        orderCount={new Set(picked.map((l) => l.shop.id)).size}
        total={linesSubtotal(picked)}
        busy={busy}
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

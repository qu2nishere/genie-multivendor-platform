// Trang Thanh toán gộp nhiều shop (prototype Checkout.jsx) — plan G10.18–G10.21, island client:load.
// Dòng được chọn ở giỏ đọc từ nháp `genie.checkout.draft`; mỗi shop thành một đơn con; voucher shop /
// sàn / freeship + Điểm dùng chung bộ tính giá với giỏ; giữ tồn kho 15 phút; lỗi hết hàng
// GNE-INV-4090 → đánh dấu dòng, cho bỏ món / về giỏ sửa số lượng rồi đặt lại.
import { useEffect, useMemo, useState } from 'react';
import {
  AddressModal,
  defaultAddress,
  fullAddress,
  useAddresses,
  type Address,
} from '@/features/account';
import { groupByShop, useCart, useCartPricing, type CartLine } from '@/features/cart';
import { extraSaving, VoucherPicker, bestVouchers } from '@/features/promotions';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import { Button, Icon, Skeleton, toast } from '@/shared/ui';
import { loadCheckoutDraft, saveCheckoutDraft } from '../application/draft';
import { usePlaceOrder } from '../application/useCheckout';
import { holdDeadline, outOfStockIds } from '../domain/checkout.rules';
import type {
  CheckoutQuote,
  CheckoutSession,
  PaymentMethod,
  PlaceOrderInput,
} from '../domain/checkout.types';
import type { CheckoutDraft } from '../domain/draft.types';
import {
  AddressCard,
  CheckoutSummary,
  PaymentMethodList,
  PlatformVoucherCard,
  PointsCard,
} from './CheckoutSections';
import { PaymentFlow } from './PaymentFlow';
import { orderTotal, ShopOrderCard } from './ShopOrderCard';

function CheckoutScreen() {
  const { t, locale } = useT();
  const cart = useCart();
  // Nháp chỉ có ở trình duyệt → đọc sau khi mount để HTML SSR và lần hydrate trùng nhau
  const [draft, setDraft] = useState<CheckoutDraft | null>(null);
  useEffect(() => setDraft(loadCheckoutDraft()), []);
  const [dead, setDead] = useState<Set<string>>(new Set());
  const [method, setMethod] = useState<PaymentMethod>('VNPAY');
  const [deadline, setDeadline] = useState(() => holdDeadline(Date.now()));
  const [picker, setPicker] = useState<
    { mode: 'platform' } | { mode: 'shop'; shopId: number } | null
  >(null);
  const [addrOpen, setAddrOpen] = useState(false);
  const [address, setAddress] = useState<Address | null>(null);
  const addresses = useAddresses(cart.guest === false);
  const place = usePlaceOrder();
  /** Phiên vừa tạo → bước thanh toán (cổng VNPAY / mã VietQR / COD). */
  const [placed, setPlaced] = useState<CheckoutSession | null>(null);

  useEffect(() => {
    if (!address && addresses.data) setAddress(defaultAddress(addresses.data));
  }, [addresses.data, address]);

  const lines = useMemo(
    () => (draft ? cart.lines.filter((l) => draft.itemIds.includes(l.id) && l.available) : []),
    [cart.lines, draft],
  );
  const live = useMemo(() => lines.filter((l) => !dead.has(l.key)), [lines, dead]);
  const groups = useMemo(() => groupByShop(lines), [lines]);
  const pricing = useCartPricing(
    live,
    groups.map((g) => g.shop.id),
  );
  const { price } = pricing;
  const saving = useMemo(
    () =>
      pricing.ready && pricing.lines.length
        ? extraSaving(pricing.lines, pricing.selection, pricing.ctx).saving
        : 0,
    [pricing.ready, pricing.lines, pricing.selection, pricing.ctx],
  );
  const notes = draft?.notes ?? {};
  const setNote = (shopId: number, v: string) => {
    const next = { ...notes, [shopId]: v };
    setDraft((d) => (d ? { ...d, notes: next } : d));
    saveCheckoutDraft({ notes: next });
  };
  const drop = (l: CartLine) => {
    const itemIds = (draft?.itemIds ?? []).filter((id) => id !== l.id);
    setDraft((d) => (d ? { ...d, itemIds } : d));
    saveCheckoutDraft({ itemIds });
    setDead((s) => {
      const n = new Set(s);
      n.delete(l.key);
      return n;
    });
  };

  const submit = () => {
    if (!address) {
      setAddrOpen(true);
      return;
    }
    const liveGroups = groupByShop(live);
    const quote: CheckoutQuote = {
      orders: liveGroups.map((g) => {
        const fee = pricing.shipOfShop(g.shop.id);
        const o = orderTotal(
          g.lines,
          price.perLine,
          fee,
          price.shopByShop[g.shop.id] ?? 0,
          price.shipCutByShop[g.shop.id] ?? 0,
        );
        return {
          shop: { id: g.shop.id, name: g.shop.name },
          items: g.lines.map((l) => ({
            cartItemId: l.id,
            productId: l.productId,
            variantId: l.variantId,
            productSlug: l.productSlug,
            title: l.title,
            image: l.image,
            variantLabel: l.variantLabel,
            price: l.price,
            qty: l.qty,
          })),
          subtotal: o.subtotal,
          shipping: fee,
          discount: o.discount,
          total: o.total,
        };
      }),
      summary: {
        subtotal: price.subtotal,
        shippingTotal: price.shipping,
        discountTotal: price.discountTotal - price.pointsDiscount,
        coinDiscount: price.pointsDiscount,
        grandTotal: price.total,
      },
      address: { recipient: address.recipient, phone: address.phone, full: fullAddress(address) },
    };
    const input: PlaceOrderInput = {
      cartItemIds: live.map((l) => l.id),
      addressId: address.id,
      vouchers: {
        platformId: price.platformVoucher?.id ?? null,
        freeshipId: price.freeshipVoucher?.id ?? null,
        shop: Object.entries(pricing.selection.shop)
          .filter(([sid]) => (price.shopByShop[Number(sid)] ?? 0) > 0)
          .map(([sid, voucherId]) => ({ shopId: Number(sid), voucherId })),
      },
      useCoins: pricing.usePoints && price.pointsDiscount > 0,
      notes: Object.entries(notes)
        .filter(([, v]) => v.trim())
        .map(([sid, note]) => ({ shopId: Number(sid), note: note.trim() })),
      paymentMethod: method,
      returnUrl: `${window.location.origin}/checkout/result`,
    };
    place.mutate(
      { input, quote },
      {
        onSuccess: (s) => setPlaced(s),
        onError: (e) => {
          if (isApiError(e) && e.code === 'GNE-INV-4090') {
            const ids = outOfStockIds(e.details);
            setDead(new Set(lines.filter((l) => ids.includes(l.id)).map((l) => l.key)));
          }
          toast.error(errorMessage(e, t));
        },
      },
    );
  };

  if (cart.pending || draft === null || (cart.loading && cart.lines.length === 0)) {
    return <CheckoutSkeleton />;
  }

  if (lines.length === 0) {
    return (
      <div className="max-w-[600px] mx-auto px-6 py-[90px] text-center">
        <Icon name="shopping-cart" size={56} className="text-subtle mx-auto mb-4" />
        <h1 className="m-0 mb-2 text-2xl font-semibold text-strong">{t('checkout.empty.title')}</h1>
        <p className="m-0 mb-[22px] text-muted">{t('checkout.empty.text')}</p>
        <Button href="/cart" iconLeft="shopping-cart">
          {t('checkout.empty.cta')}
        </Button>
      </div>
    );
  }

  const canPlace = live.length > 0 && pricing.ready && !place.isPending;
  return (
    <div className="mx-auto max-w-[1280px] px-6 pt-[18px] pb-[60px] max-md:px-3">
      <h1 className="m-0 mb-4 text-3xl font-bold text-strong">{t('checkout.title')}</h1>
      <div className="grid grid-cols-[minmax(0,1fr)_380px] gap-5 items-start max-[900px]:grid-cols-1">
        <div>
          <AddressCard
            address={address}
            loading={addresses.isPending}
            onChange={() => setAddrOpen(true)}
          />
          {groups.map((g, i) => (
            <ShopOrderCard
              key={g.shop.id}
              group={g}
              index={i + 1}
              count={groups.length}
              dead={dead}
              perLine={price.perLine}
              shopCut={price.shopByShop[g.shop.id] ?? 0}
              shipCut={price.shipCutByShop[g.shop.id] ?? 0}
              shipFee={pricing.shipOfShop(g.shop.id)}
              freeByShop={pricing.freeByShop(g.shop.id)}
              note={notes[g.shop.id] ?? ''}
              onNote={(v) => setNote(g.shop.id, v)}
              onShopVoucher={() => setPicker({ mode: 'shop', shopId: g.shop.id })}
              onDrop={drop}
            />
          ))}
          <PlatformVoucherCard
            price={price}
            extraSaving={saving}
            onOpen={() => setPicker({ mode: 'platform' })}
            onBest={() => pricing.setSelection(bestVouchers(pricing.lines, pricing.ctx))}
          />
          <PointsCard
            balance={pricing.pointsBalance}
            price={price}
            on={pricing.usePoints}
            onToggle={() => pricing.setUsePoints(!pricing.usePoints)}
          />
          <PaymentMethodList value={method} onChange={setMethod} />
        </div>
        <CheckoutSummary
          price={price}
          orderCount={groupByShop(live).length}
          itemCount={live.length}
          method={method}
          canPlace={canPlace}
          placing={place.isPending}
          deadline={deadline}
          onRenew={() => setDeadline(holdDeadline(Date.now()))}
          onPlace={submit}
        />
      </div>

      <div className="tr-paybar">
        <div>
          <small>{t('cart.bar.total')}</small>
          <b>{formatVND(price.total, { locale })}</b>
        </div>
        <Button size="lg" disabled={!canPlace} loading={place.isPending} onClick={submit}>
          {t('checkout.place.short')}
        </Button>
      </div>

      {placed && <PaymentFlow session={placed} onClose={() => setPlaced(null)} />}
      <AddressModal
        open={addrOpen}
        onClose={() => setAddrOpen(false)}
        selectedId={address?.id ?? null}
        onSelect={setAddress}
      />
      <VoucherPicker
        open={picker !== null}
        onClose={() => setPicker(null)}
        title={t(picker?.mode === 'shop' ? 'cart.picker.shop_title' : 'cart.picker.platform_title')}
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
    </div>
  );
}

function CheckoutSkeleton() {
  const { t } = useT();
  return (
    <div className="mx-auto max-w-[1280px] px-6 pt-[18px] max-md:px-3" aria-busy="true">
      <h1 className="m-0 mb-4 text-3xl font-bold text-strong">{t('checkout.title')}</h1>
      <div className="grid grid-cols-[minmax(0,1fr)_380px] gap-5 max-[900px]:grid-cols-1">
        <div className="grid gap-3.5">
          <Skeleton className="h-[76px]" rounded="lg" />
          <Skeleton className="h-[260px]" rounded="lg" />
        </div>
        <Skeleton className="h-[360px]" rounded="lg" />
      </div>
    </div>
  );
}

export default function CheckoutPage() {
  return (
    <Providers>
      <CheckoutScreen />
    </Providers>
  );
}

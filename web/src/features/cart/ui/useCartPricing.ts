// Ghép bộ tính giá cho Giỏ hàng và Thanh toán (plan G10.14–G10.15): kho voucher (promotions), phí ship
// (checkout), số dư Điểm (coins) + lựa chọn lưu trong nháp checkout. Ghép ở tầng ui vì luật phụ thuộc
// chỉ cho ui gọi API công khai của feature khác (TS 5.2).
import { useStore } from '@nanostores/react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  loadCheckoutDraft,
  makeShipFn,
  saveCheckoutDraft,
  useShipPolicies,
} from '@/features/checkout';
import { useCoins } from '@/features/coins';
import {
  bestVouchers,
  priceCart,
  selectionFromCodes,
  selectionToCodes,
  useMyVouchers,
  type PriceBreakdown,
  type PricingContext,
  type PricingLine,
  type Voucher,
  type VoucherSelection,
} from '@/features/promotions';
import { $session } from '@/shared/auth/session';
import type { CartLine } from '../domain/cart.types';

export const toPricingLine = (l: CartLine): PricingLine => ({
  key: l.key,
  shopId: l.shop.id,
  category: l.category,
  price: l.price,
  qty: l.qty,
});

export interface CartPricing {
  ready: boolean;
  ctx: PricingContext;
  lines: PricingLine[];
  selection: VoucherSelection;
  setSelection: (s: VoucherSelection) => void;
  usePoints: boolean;
  setUsePoints: (on: boolean) => void;
  pointsBalance: number;
  /** Số Điểm dùng được với các dòng hiện tại. */
  pointsUsable: number;
  price: PriceBreakdown;
  /** Phí ship của shop với các dòng đang chọn; policy để biết shop tự miễn phí. */
  shipOfShop: (shopId: number) => number;
  freeByShop: (shopId: number) => boolean;
  addVoucher: (v: Voucher) => void;
}

/**
 * @param picked dòng đang chọn để tính giá.
 * @param allShopIds shop của mọi dòng trong giỏ (nạp phí ship một lần).
 */
export function useCartPricing(picked: CartLine[], allShopIds: number[]): CartPricing {
  const session = useStore($session);
  const authed = session.status === 'authenticated';
  const vouchersQ = useMyVouchers(authed);
  const coinsQ = useCoins(authed);
  const policiesQ = useShipPolicies(allShopIds);
  const [extra, setExtra] = useState<Voucher[]>([]);
  const [selection, setSel] = useState<VoucherSelection | null>(null);
  const [usePoints, setUse] = useState(() => loadCheckoutDraft().usePoints);

  const policies = useMemo(() => policiesQ.data ?? {}, [policiesQ.data]);
  const ctx = useMemo<PricingContext>(
    () => ({
      vouchers: [
        ...(vouchersQ.data ?? []),
        ...extra.filter((e) => !vouchersQ.data?.some((v) => v.id === e.id)),
      ],
      ship: makeShipFn(policies),
    }),
    [vouchersQ.data, extra, policies],
  );
  const lines = useMemo(() => picked.map(toPricingLine), [picked]);
  const ready = !authed || (vouchersQ.isSuccess && policiesQ.isSuccess);

  // Lựa chọn ban đầu: mã trong nháp (người dùng đã chọn) hoặc bộ mã giảm nhiều nhất
  useEffect(() => {
    if (selection || !ready || lines.length === 0) return;
    const codes = loadCheckoutDraft().voucherCodes;
    setSel(codes ? selectionFromCodes(codes, ctx.vouchers) : bestVouchers(lines, ctx));
  }, [ready, lines, ctx, selection]);

  const sel = useMemo<VoucherSelection>(
    () => selection ?? { platform: null, freeship: null, shop: {} },
    [selection],
  );
  const balance = coinsQ.data?.balance ?? 0;
  const price = useMemo(
    () => priceCart(lines, sel, ctx, { use: usePoints, balance }),
    [lines, sel, ctx, usePoints, balance],
  );

  const setSelection = useCallback(
    (s: VoucherSelection) => {
      setSel(s);
      saveCheckoutDraft({ voucherCodes: selectionToCodes(s, ctx.vouchers) });
    },
    [ctx.vouchers],
  );
  const setUsePoints = useCallback((on: boolean) => {
    setUse(on);
    saveCheckoutDraft({ usePoints: on });
  }, []);

  return {
    ready,
    ctx,
    lines,
    selection: sel,
    setSelection,
    usePoints,
    setUsePoints,
    pointsBalance: balance,
    pointsUsable: Math.min(balance, price.pointsCap),
    price,
    shipOfShop: (shopId) => ctx.ship(shopId, lines),
    freeByShop: (shopId) => {
      const p = policies[shopId];
      return !!p && p.fee > 0 && ctx.ship(shopId, lines) === 0;
    },
    addVoucher: (v) => setExtra((x) => [...x.filter((y) => y.id !== v.id), v]),
  };
}

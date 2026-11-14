import type {
  CheckoutSession,
  PaymentInstruction,
  PlaceOrderInput,
} from '../domain/checkout.types';
import type { CheckoutSessionDto, PaymentDto, PlaceOrderDto } from './checkout.dto';

export const toPayment = (p: PaymentDto): PaymentInstruction =>
  p.method === 'VNPAY'
    ? { method: 'VNPAY', paymentUrl: p.payment_url }
    : p.method === 'VIETQR'
      ? {
          method: 'VIETQR',
          vietqr: {
            bankName: p.vietqr.bank_name,
            accountNumber: p.vietqr.account_number,
            accountName: p.vietqr.account_name,
            amount: p.vietqr.amount,
            transferContent: p.vietqr.transfer_content,
            qrPayload: p.vietqr.qr_payload,
            expiresAt: p.vietqr.expires_at,
          },
        }
      : { method: 'COD' };

export const toCheckoutSession = (d: CheckoutSessionDto): CheckoutSession => ({
  id: d.id,
  code: d.code,
  status: d.status,
  paymentMethod: d.payment_method,
  createdAt: d.created_at,
  reservationExpiresAt: d.reservation_expires_at,
  orders: d.orders.map((o) => ({
    id: o.id,
    code: o.code,
    shop: o.shop,
    grandTotal: o.grand_total,
  })),
  summary: {
    subtotal: d.summary.subtotal,
    shippingTotal: d.summary.shipping_total,
    discountTotal: d.summary.discount_total,
    coinDiscount: d.summary.coin_discount,
    grandTotal: d.summary.grand_total,
  },
  payment: d.payment ? toPayment(d.payment) : null,
});

export const toPlaceOrderDto = (i: PlaceOrderInput): PlaceOrderDto => ({
  cart_item_ids: i.cartItemIds,
  address_id: i.addressId,
  vouchers: {
    platform_voucher_id: i.vouchers.platformId,
    freeship_voucher_id: i.vouchers.freeshipId,
    shop_vouchers: i.vouchers.shop.map((s) => ({ shop_id: s.shopId, voucher_id: s.voucherId })),
  },
  use_coins: i.useCoins,
  notes: i.notes.map((n) => ({ shop_id: n.shopId, note: n.note })),
  payment_method: i.paymentMethod,
  return_url: i.returnUrl,
});

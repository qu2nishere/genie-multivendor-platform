import type {
  CheckoutSession,
  PaymentInstruction,
  PlaceOrderInput,
} from '../domain/checkout.types';
import type { CheckoutSessionDto, PaymentDto, PlaceOrderDto } from './checkout.dto';

/**
 * Khối payment → hướng dẫn thanh toán. Cổng chưa trả liên kết / mã QR (cổng giả lập trước chuỗi C3) → null: giao diện
 * chuyển thẳng sang trang kết quả (phiên chờ thanh toán, tự hủy khi hết 15 phút — UC-ORD-08).
 */
export const toPayment = (p: PaymentDto): PaymentInstruction | null =>
  p.method === 'VNPAY'
    ? p.payment_url
      ? { method: 'VNPAY', paymentUrl: p.payment_url }
      : null
    : p.method === 'VIETQR'
      ? p.vietqr && {
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

/** @param withDiscounts false khi backend chưa áp voucher / Điểm (plan C2 D2) → không gửi mã voucher của mock. */
export const toPlaceOrderDto = (i: PlaceOrderInput, withDiscounts = true): PlaceOrderDto => ({
  cart_item_ids: i.cartItemIds,
  address_id: i.addressId,
  vouchers: withDiscounts
    ? {
        platform_voucher_id: i.vouchers.platformId,
        freeship_voucher_id: i.vouchers.freeshipId,
        shop_vouchers: i.vouchers.shop.map((s) => ({ shop_id: s.shopId, voucher_id: s.voucherId })),
      }
    : null,
  use_coins: withDiscounts && i.useCoins,
  notes: i.notes.map((n) => ({ shop_id: n.shopId, note: n.note })),
  payment_method: i.paymentMethod,
  return_url: i.returnUrl,
});

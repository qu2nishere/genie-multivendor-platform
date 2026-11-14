// ProductSummary (miền) → dữ liệu thẻ của shared/ui ProductCard.
import type { ProductCardData } from '@/shared/ui';
import { productHref } from '../domain/product.rules';
import type { ProductSummary } from '../domain/product.types';

/** Nhãn góc thẻ do UI dịch: "Shop đang tạm nghỉ" ưu tiên hơn "Đặt trước" (prototype CardX). */
export function toCardData(
  p: ProductSummary,
  labels: { vacation: string; preorder: string },
): ProductCardData {
  return {
    id: p.id,
    href: productHref(p),
    title: p.title,
    image: p.image,
    price: p.price,
    original: p.originalPrice ?? undefined,
    discountPct: p.discountPct,
    rating: p.rating,
    shopId: p.shopId,
    shopName: p.shopName,
    freeship: p.freeship,
    flag: p.shopOnVacation ? labels.vacation : p.preorderDays ? labels.preorder : undefined,
    flash: p.flash ?? undefined,
  };
}

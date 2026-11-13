// Nhóm phân loại + SKU của một sản phẩm cho modal đổi phân loại ở giỏ (API_SPEC 5.10).
import { useQuery } from '@tanstack/react-query';
import { getVariantChoice } from '../infrastructure/cart.api';

export function useVariantChoice(productId: number | null) {
  return useQuery({
    queryKey: ['product', productId, 'variants'],
    queryFn: () => getVariantChoice(productId ?? 0),
    enabled: productId != null,
    staleTime: 5 * 60_000,
  });
}

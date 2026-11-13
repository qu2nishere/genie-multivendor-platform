// Dữ liệu cho "Sản phẩm yêu thích": sản phẩm theo tập id yêu thích + chi tiết (chọn phân loại mặc định).
import { useQuery } from '@tanstack/react-query';
import { getProduct, listProductsByIds } from '../infrastructure/product.api';

export function useWishlistProducts(ids: number[]) {
  const key = [...ids].sort((a, b) => a - b).join(',');
  return useQuery({
    queryKey: ['wishlist', 'products', key],
    queryFn: () => listProductsByIds(ids),
    placeholderData: (prev) => prev,
  });
}

export const loadProductDetail = (id: number) => getProduct(id);

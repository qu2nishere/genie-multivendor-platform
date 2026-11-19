// Dữ liệu cho "Sản phẩm yêu thích" (UC-CAT-08): GET /me/wishlist (giá, SKU mặc định, còn mua được) + chi tiết sản
// phẩm khi bấm "Thêm vào giỏ". Khóa query gồm tập id đang thích để bỏ thích ở nơi khác thì danh sách tải lại.
import { useQuery } from '@tanstack/react-query';
import { getProduct } from '../infrastructure/product.api';
import { listWishlist } from '../infrastructure/wishlist.api';

export function useWishlistEntries(ids: Set<number>) {
  const key = [...ids].sort((a, b) => a - b).join(',');
  return useQuery({
    queryKey: ['wishlist', 'entries', key],
    queryFn: listWishlist,
    placeholderData: (prev) => prev,
  });
}

export const loadProductDetail = (id: number) => getProduct(id);

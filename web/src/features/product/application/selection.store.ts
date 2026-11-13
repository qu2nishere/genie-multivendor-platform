// Lựa chọn phân loại đang chọn trên trang sản phẩm — khối mua ghi, nút "Chia sẻ" đọc (hai island).
import { atom } from 'nanostores';
import type { Selection } from '../domain/product.rules';

export const $selection = atom<Selection>({});

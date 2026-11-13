// Ngăn kéo bộ lọc ở màn hẹp: nút "Bộ lọc" (SortBar) và FilterPanel là hai island riêng,
// dùng chung trạng thái mở qua nanostore.
import { atom } from 'nanostores';

export const $filtersOpen = atom(false);
export const openFilters = () => $filtersOpen.set(true);
export const closeFilters = () => $filtersOpen.set(false);

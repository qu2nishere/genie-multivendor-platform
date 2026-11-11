// Shop mẫu (prototype data.jsx V + FOLLOWS) từ bộ seed chung; trạng thái theo dõi giữ trong phiên mock.
import { SEED_SHOPS } from '@/shared/api/mock/seed';
import { productImg } from '@/shared/lib/assets';
import type { ShopDto } from './shop.dto';

const followed = new Set<number>(SEED_SHOPS.filter((s) => s.followed).map((s) => s.id));

export const shopDtos = (): ShopDto[] =>
  SEED_SHOPS.map(({ code: _code, cover_image, ...s }) => ({
    ...s,
    cover_image: productImg(cover_image),
    vacation: s.vacation ? { ...s.vacation } : null,
    followed: followed.has(s.id),
  }));

export function setMockFollow(id: number, on: boolean): void {
  if (on) followed.add(id);
  else followed.delete(id);
}

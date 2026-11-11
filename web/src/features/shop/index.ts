// API công khai của feature "shop".
export type { Shop, ShopVacation } from './domain/shop.types';
export {
  shopHref,
  shopSearchHref,
  isOnVacation,
  canOrderFrom,
  shopShipFee,
} from './domain/shop.rules';
export { getShop, listShops, listFollowedShops, setFollow } from './infrastructure/shop.api';

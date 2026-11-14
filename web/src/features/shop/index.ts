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
export { useFollowedShops, useSetFollow, FOLLOWED_SHOPS_KEY } from './application/useFollow';
export { default as ShopStrip, type ShopStripProps } from './ui/ShopStrip';
export { FollowButton, ChatButton, type FollowButtonProps } from './ui/FollowButton';
export { default as ShopActions } from './ui/ShopActions';

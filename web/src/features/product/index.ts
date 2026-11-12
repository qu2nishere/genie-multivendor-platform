// API công khai của feature "product".
export type {
  Product,
  ProductSummary,
  ProductQuery,
  ProductSort,
  ProductBadge,
  ProductSpec,
  VariantGroup,
  VariantOption,
  FlashInfo,
  StockState,
} from './domain/product.types';
export {
  productHref,
  discountPct,
  isFlashSoldOut,
  badges,
  priceOf,
  stockOf,
  stockState,
  missingGroups,
  variantLabel,
  maxQty,
  needsCanonicalRedirect,
  encodeSelection,
  decodeSelection,
  LOW_STOCK,
  type Selection,
} from './domain/product.rules';
export {
  listProducts,
  getProduct,
  listRelated,
  listProductsByIds,
  listFeed,
} from './infrastructure/product.api';
export { $wishlist, loadWishlist, toggleWishlist } from './application/wishlist.store';
export { default as ProductGrid, ProductGridBody, type ProductGridProps } from './ui/ProductGrid';
export { default as ProductFeed, type ProductFeedProps } from './ui/ProductFeed';
export { WishHeart } from './ui/WishHeart';
export { toCardData } from './ui/toCard';

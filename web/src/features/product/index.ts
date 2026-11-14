// API công khai của feature "product".
export type {
  Product,
  ProductSummary,
  ProductQuery,
  ProductFacets,
  ProductSort,
  ProductBadge,
  ProductSpec,
  ProductSku,
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
  skuOf,
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
  getFacets,
} from './infrastructure/product.api';
export { $wishlist, loadWishlist, toggleWishlist } from './application/wishlist.store';
export { default as ProductGrid, ProductGridBody, type ProductGridProps } from './ui/ProductGrid';
export { default as ProductFeed, type ProductFeedProps } from './ui/ProductFeed';
export { WishHeart } from './ui/WishHeart';
export { toCardData } from './ui/toCard';
export { default as ProductGallery, type ProductGalleryProps } from './ui/ProductGallery';
export { default as WishShare, type WishShareProps } from './ui/WishShare';
export { default as BuyBox, type BuyBoxProps } from './ui/BuyBox';
export { default as WishlistPanel } from './ui/WishlistPanel';

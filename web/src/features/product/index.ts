// API công khai của feature "product".
export type {
  Product,
  ProductSummary,
  ProductQuery,
  ProductFacets,
  FacetAttribute,
  ProductSort,
  ProductBadge,
  ProductSpec,
  ProductSku,
  CategoryCrumb,
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
  stockFor,
  isOptionOut,
  shareVariantParam,
  decodeShared,
  viewSourceOf,
  LOW_STOCK,
  type Selection,
  type ViewSource,
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
export { trackProductView } from './application/viewTracking';
export { default as ProductGrid, ProductGridBody, type ProductGridProps } from './ui/ProductGrid';
export { default as ProductFeed, type ProductFeedProps } from './ui/ProductFeed';
export { WishHeart } from './ui/WishHeart';
export { toCardData } from './ui/toCard';
export { default as ProductGallery, type ProductGalleryProps } from './ui/ProductGallery';
export { default as WishShare, type WishShareProps } from './ui/WishShare';
export { default as BuyBox, type BuyBoxProps } from './ui/BuyBox';
export { default as WishlistPanel } from './ui/WishlistPanel';

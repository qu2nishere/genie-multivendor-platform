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
} from './infrastructure/product.api';

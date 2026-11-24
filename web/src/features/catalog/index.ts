// API công khai của feature "catalog" — nơi DUY NHẤT feature khác / pages được import.
export type { Category, CategoryTree } from './domain/catalog.types';
export { categoryHref, buildTree, categoryPath } from './domain/catalog.rules';
export {
  listCategories,
  listRootCategories,
  getCategoryBySlug,
  mockProductIdsOfSubcategory,
  listVerifiedBrands,
} from './infrastructure/catalog.api';

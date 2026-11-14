// API công khai của feature "admin-catalog" — danh mục toàn sàn (/admin/catalog), bộ thuộc tính
// (/admin/catalog/:id/attributes), sản phẩm theo danh mục con (/admin/catalog/sub/:id/products).
export type { RootCategory, CategoryAttribute, CategoryProposal } from './domain/catalog.types';
export { default as CatalogPage } from './ui/CatalogPage';
export { default as AttributePanel } from './ui/AttributePanel';
export { default as CategoryProducts } from './ui/CategoryProducts';

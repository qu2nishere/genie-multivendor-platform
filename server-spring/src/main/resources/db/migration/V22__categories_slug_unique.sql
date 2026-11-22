-- V22 — Slug danh mục duy nhất (plan B2 D3; UC-CAT-01, UC-CAT-14; API 5.2, 11.11 GNE-CAT-4091)
--
-- Trang /c/{slug} và GET /public/categories/{slug} tìm danh mục theo slug → slug phải duy nhất. V2 để cột slug NULLABLE,
-- không UNIQUE; chỉ thêm ràng buộc (không thêm cột). Chỉ mục một phần (WHERE slug IS NOT NULL) để các dòng cũ chưa có
-- slug không chặn migration — API luôn gán slug (tự sinh từ tên khi bỏ trống). Seed R__seed_demo_10 chống trùng theo slug.

CREATE UNIQUE INDEX uk_categories_slug ON categories (slug) WHERE slug IS NOT NULL;

# 0006 — Thư mục / tệp bổ sung so với cây TECH_STACK 2.4

- **Trạng thái:** Chấp nhận (plan mục 3, chốt lại theo cây thực tế khi bàn giao 2026-11-14)
- **Phạm vi:** `web/`

## Bối cảnh

Cây `web/` ở TECH_STACK 2.4 chưa có chỗ cho một số màn / hạ tầng của prototype. Cây gốc giữ nguyên;
dưới đây là phần thêm và lý do.

## Quyết định

```
web/
├── .env.example · .prettierrc.mjs · .lintstagedrc.mjs · vitest.config.ts · playwright.config.ts
├── public/
│   ├── fonts/                 # Be Vietnam Pro + IBM Plex Mono; fonts/themes/ = font 8 theme (OFL)
│   └── img/{products, banners, payments, themes, mock}/
├── tests/e2e/                 # Playwright: seo, guest, checkout-vnpay, orders, kyc-onboarding, admin
└── src/
    ├── app/{seller,admin}/    # SPA: router lazy, khung (Layout), cổng KYC / quyền; app/providers.tsx
    ├── assets/brand/          # SVG thương hiệu import qua Astro
    ├── layouts/
    │   ├── AuthLayout.astro · AppShellLayout.astro (Seller / Admin / dev)
    │   ├── partials/          # SeoHead.astro, ThemeBoot.astro — dùng chung 3 layout
    │   └── storefront/        # Header.tsx, Footer.astro, LangSelect, AuthTools, ToastHost, ThemePickerSlot
    ├── pages/
    │   ├── become-seller.astro · flash-sale.astro · campaign/[slug].astro · chat.astro
    │   ├── robots.txt.ts      # robots sinh động (dòng Sitemap tuyệt đối theo tên miền)
    │   └── dev/ui.astro       # thư viện component nội bộ, 404 ở bản build
    ├── features/              # 34 feature, mỗi feature 4 tầng + index.ts (khuôn plan 3.1)
    │   ├── <x>/server.ts      # API công khai cho component .astro (ESLint chặn .tsx import)
    │   ├── policies · seller-* (9) · admin-* (8)   # màn prototype chưa có chỗ trong cây
    ├── shared/
    │   ├── ui/{icon, brand, dash}/   # Icon + bảng ICONS · GenieMark/Logo/Coin, ThemePicker · bộ thống kê
    │   ├── hooks/             # useT, useCountdown, useClickOutside, useMediaQuery, useFocusTrap, useScrollLock
    │   ├── api/mock.ts        # USE_MOCK, mockDelay, mockFail, mockState, phân trang page / cursor
    │   ├── api/mock/          # dữ liệu mock dùng chung nhiều feature (seed, seller, admin, banners…)
    │   ├── api/upload.ts      # URL ký sẵn → PUT MinIO → object_key (API 4.1)
    │   └── auth/permissions.ts  # 13 mã quyền + bộ quyền theo chức danh
    └── styles/
        ├── tokens.css · base.css · responsive.css · dash.css · themes.css · theme-fonts.css
        └── storefront/*.css   # storefront.css của prototype tách theo khối, bọc @layer components
```

- Feature mới phải đủ 4 tầng + `index.ts` và khớp mẫu `src/features/*/…` của ESLint boundaries.
- Tầng chưa có tệp giữ `.gitkeep` (hiện còn `catalog/application`, `catalog/ui`,
  `payment/application`, `payment/infrastructure`).

## Hệ quả

- ESLint boundaries khai thêm phần tử `app`, `layouts`, `shared-hooks`, `shared/auth/permissions.ts`
  (dùng được trong `domain`), `features-public` (`index.ts`, `server.ts`).
- Feature trong cùng nhóm import tương đối; khác feature chỉ qua `index.ts` / `server.ts`.

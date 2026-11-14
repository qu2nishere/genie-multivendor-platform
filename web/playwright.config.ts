import { defineConfig, devices } from '@playwright/test';

// E2E — hai khổ màn 360 px và 1440 px (plan G01.03), dữ liệu mock (PUBLIC_USE_MOCK mặc định bật).
// Mặc định chạy trên BẢN BUILD production (astro build → node dist/server/entry.mjs) ở cổng 4322:
// giống thật hơn và tránh lỗi ERR_INSUFFICIENT_RESOURCES khi nhiều trình duyệt cùng tải hàng trăm
// module ESM rời của dev server (G13.13). E2E_DEV=1 → chạy trên `astro dev` (cổng 4321).
// E2E_PORT đổi cổng khi cổng mặc định đang bận.
const DEV = !!process.env.E2E_DEV;
const PORT = Number(process.env.E2E_PORT ?? (DEV ? 4321 : 4322));
const BASE = `http://localhost:${PORT}`;

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  // Nhiều worker trên máy dev Windows làm Chromium crash (0xC0000409) → giới hạn 2
  workers: process.env.CI ? 1 : 2,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: BASE,
    trace: 'on-first-retry',
  },
  webServer: {
    command: DEV
      ? `npm run dev -- --port ${PORT}`
      : `npx astro build && node ./dist/server/entry.mjs`,
    env: { HOST: 'localhost', PORT: String(PORT) },
    url: BASE,
    reuseExistingServer: !process.env.CI,
    timeout: 300_000,
  },
  projects: [
    {
      name: 'mobile-360',
      use: { ...devices['Pixel 5'], viewport: { width: 360, height: 780 } },
    },
    {
      name: 'desktop-1440',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
  ],
});

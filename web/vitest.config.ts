import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';

// Vitest cho domain / application (TECH_STACK mục 14) — không quét tests/e2e của Playwright
export default defineConfig({
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  test: {
    include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
    environment: 'node',
    passWithNoTests: false,
  },
});

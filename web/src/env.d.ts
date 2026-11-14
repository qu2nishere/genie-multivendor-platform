/// <reference path="../.astro/types.d.ts" />
/// <reference types="astro/client" />

declare namespace App {
  interface Locals {
    lang: 'vi' | 'en';
  }
}

interface ImportMetaEnv {
  readonly PUBLIC_WEB_URL?: string;
  readonly PUBLIC_API_URL?: string;
  readonly PUBLIC_S3_URL?: string;
  readonly INTERNAL_API_URL?: string;
  readonly PUBLIC_USE_MOCK?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// tsc thuần (npm run check) không đọc được .astro; astro check vẫn dùng kiểu thật của từng file.
declare module '*.astro' {
  const Component: (props: Record<string, unknown>) => unknown;
  export default Component;
}

/// <reference path="../.astro/types.d.ts" />
/// <reference types="astro/client" />

declare namespace App {
  interface Locals {
    lang: 'vi' | 'en';
  }
}

interface ImportMetaEnv {
  readonly PUBLIC_API_URL: string;
  readonly PUBLIC_S3_URL: string;
  readonly INTERNAL_API_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// Prettier — nạp plugin bằng import để chạy được cả khi cwd là gốc monorepo (hook lint-staged)
import * as astro from 'prettier-plugin-astro';

/** @type {import('prettier').Config} */
export default {
  plugins: [astro],
  singleQuote: true,
  semi: true,
  trailingComma: 'all',
  printWidth: 100,
  overrides: [{ files: '*.astro', options: { parser: 'astro' } }],
};

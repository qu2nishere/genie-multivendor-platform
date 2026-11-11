// lint-staged cho web/ — gọi thẳng binary trong web/node_modules (hook chạy từ gốc repo, Windows không có eslint trên PATH)
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const web = dirname(fileURLToPath(import.meta.url));
const bin = (pkg, file) => `node "${join(web, 'node_modules', pkg, file)}"`;
const quote = (files) => files.map((f) => `"${f}"`).join(' ');

export default {
  '*.{ts,tsx,astro,js,mjs,cjs}': (files) =>
    `${bin('eslint', 'bin/eslint.js')} --resolve-plugins-relative-to "${web}" --fix ${quote(files)}`,
  '*.{ts,tsx,astro,json,css,md}': (files) =>
    `${bin('prettier', 'bin/prettier.cjs')} --write ${quote(files)}`,
};

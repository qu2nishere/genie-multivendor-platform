#!/usr/bin/env node
// So khóa web/src/i18n/vi.json và en.json — CI báo lỗi khi thiếu khóa (TECH_STACK mục 3.4)
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = join(dirname(fileURLToPath(import.meta.url)), '..', 'web', 'src', 'i18n');
const load = (lang) => JSON.parse(readFileSync(join(dir, `${lang}.json`), 'utf8'));

function keys(obj, prefix = '') {
  return Object.entries(obj).flatMap(([k, v]) =>
    v && typeof v === 'object' ? keys(v, `${prefix}${k}.`) : [`${prefix}${k}`],
  );
}

const vi = new Set(keys(load('vi')));
const en = new Set(keys(load('en')));
const missingEn = [...vi].filter((k) => !en.has(k));
const missingVi = [...en].filter((k) => !vi.has(k));

if (missingEn.length || missingVi.length) {
  if (missingEn.length) console.error('Thiếu trong en.json:\n  ' + missingEn.join('\n  '));
  if (missingVi.length) console.error('Thiếu trong vi.json:\n  ' + missingVi.join('\n  '));
  process.exit(1);
}
console.log(`i18n OK — ${vi.size} khóa.`);

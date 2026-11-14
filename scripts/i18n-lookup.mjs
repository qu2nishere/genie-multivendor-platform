#!/usr/bin/env node
// Tra bản dịch tiếng Anh của prototype (plan G04.11) — dùng khi điền web/src/i18n/en.json.
// Prototype (genie/i18n.js) lấy CHUỖI TIẾNG VIỆT làm khóa: const EN = { 'Tiếng Việt': 'English', … }.
//
//   node scripts/i18n-lookup.mjs "Genie đang giữ tiền" "Đơn hàng gần đây"
//   node scripts/i18n-lookup.mjs --missing          # liệt kê giá trị vi.json chưa có bản dịch prototype
//   node scripts/i18n-lookup.mjs --fill             # in ra en.json đề xuất (chỉ điền khóa còn trùng tiếng Việt)
//   I18N_SRC=/đường/dẫn/i18n.js node scripts/i18n-lookup.mjs …
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const src =
  process.env.I18N_SRC ?? join(root, '..', 'MultivendorEcommercePrototype', 'genie', 'i18n.js');
if (!existsSync(src)) {
  console.error(`Không thấy từ điển prototype: ${src}\nĐặt biến I18N_SRC trỏ tới genie/i18n.js.`);
  process.exit(2);
}

// Cắt object literal `const EN = { … };` rồi đánh giá riêng phần đó (chỉ chứa chuỗi).
const text = readFileSync(src, 'utf8');
const start = text.indexOf('const EN = {');
if (start < 0) {
  console.error('Không tìm thấy "const EN = {" trong i18n.js');
  process.exit(2);
}
let depth = 0;
let end = -1;
for (let i = text.indexOf('{', start); i < text.length; i++) {
  const c = text[i];
  if (c === "'" || c === '"' || c === '`') {
    const q = c;
    for (i++; i < text.length && text[i] !== q; i++) if (text[i] === '\\') i++;
    continue;
  }
  if (c === '{') depth++;
  if (c === '}' && --depth === 0) {
    end = i + 1;
    break;
  }
}
const EN = new Function(`return ${text.slice(text.indexOf('{', start), end)};`)();
const norm = (s) => s.replace(/\s+/g, ' ').trim();
const dict = new Map(Object.entries(EN).map(([k, v]) => [norm(k), v]));
const lookup = (s) => dict.get(norm(s));

const args = process.argv.slice(2);
const webI18n = join(root, 'web', 'src', 'i18n');
const flat = (obj, p = '') =>
  Object.entries(obj).flatMap(([k, v]) =>
    v && typeof v === 'object' ? flat(v, `${p}${k}.`) : [[`${p}${k}`, v]],
  );

if (args[0] === '--missing' || args[0] === '--fill') {
  const vi = JSON.parse(readFileSync(join(webI18n, 'vi.json'), 'utf8'));
  const en = JSON.parse(readFileSync(join(webI18n, 'en.json'), 'utf8'));
  const enFlat = new Map(flat(en));
  if (args[0] === '--missing') {
    for (const [k, v] of flat(vi)) if (!lookup(v)) console.log(`${k}\t${v}`);
  } else {
    const set = (obj, path, val) => {
      const parts = path.split('.');
      let o = obj;
      parts.slice(0, -1).forEach((p) => (o = o[p] ??= {}));
      o[parts.at(-1)] = val;
    };
    for (const [k, v] of flat(vi)) {
      const cur = enFlat.get(k);
      const hit = lookup(v);
      if ((cur === undefined || cur === v) && hit) set(en, k, hit);
    }
    console.log(JSON.stringify(en, null, 2));
  }
} else if (args.length) {
  for (const s of args) console.log(`${s}\t→\t${lookup(s) ?? '(không có trong từ điển prototype)'}`);
} else {
  console.log(`Từ điển prototype: ${dict.size} chuỗi. Dùng: node scripts/i18n-lookup.mjs "<chuỗi tiếng Việt>"`);
}

// Hiển thị HTML của thông báo sàn (plan 2026-11-26 thông báo D13, N05.06): server đã lọc khi lưu; đây là lớp phòng
// thủ phía client — chỉ giữ danh sách thẻ / thuộc tính giống server, liên kết https / tương đối, ảnh https.
import { useMemo } from 'react';
import { cn } from '@/shared/lib/cn';

const TAGS = new Set([
  'P',
  'BR',
  'B',
  'STRONG',
  'I',
  'EM',
  'U',
  'S',
  'UL',
  'OL',
  'LI',
  'BLOCKQUOTE',
  'H2',
  'H3',
  'A',
  'IMG',
]);
const ATTRS: Record<string, string[]> = { A: ['href'], IMG: ['src', 'alt'] };

/** Lọc chuỗi HTML bằng DOMParser của trình duyệt; SSR (không có DOMParser) → bỏ hết thẻ. */
export function cleanHtml(html: string): string {
  if (typeof DOMParser === 'undefined') return html.replace(/<[^>]*>/g, '');
  const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html');
  const root = doc.body.firstElementChild;
  if (!root) return '';
  const walk = (el: Element) => {
    for (const child of Array.from(el.children)) {
      if (!TAGS.has(child.tagName)) {
        // thẻ lạ: script / style / iframe bỏ hẳn; thẻ khác giữ chữ bên trong
        if (['SCRIPT', 'STYLE', 'IFRAME', 'OBJECT', 'EMBED'].includes(child.tagName)) {
          child.remove();
        } else {
          walk(child); // lọc con trước khi bỏ thẻ bọc — con được đẩy ra ngoài cũng đã sạch
          child.replaceWith(...Array.from(child.childNodes));
        }
        continue;
      }
      for (const a of Array.from(child.attributes)) {
        if (!(ATTRS[child.tagName] ?? []).includes(a.name)) child.removeAttribute(a.name);
      }
      if (child.tagName === 'A') {
        const href = child.getAttribute('href') ?? '';
        if (!(href.startsWith('https://') || (href.startsWith('/') && !href.startsWith('//')))) {
          walk(child);
          child.replaceWith(...Array.from(child.childNodes));
          continue;
        }
        child.setAttribute('rel', 'noopener noreferrer');
        child.setAttribute('target', '_blank');
      }
      if (child.tagName === 'IMG' && !(child.getAttribute('src') ?? '').startsWith('https://')) {
        // máy dev dùng http://localhost cho Silo — chấp nhận cả http khi trang cũng là http
        const src = child.getAttribute('src') ?? '';
        if (!(window.location.protocol === 'http:' && src.startsWith('http://'))) {
          child.remove();
          continue;
        }
      }
      walk(child);
    }
  };
  walk(root);
  return root.innerHTML;
}

export function SafeHtml({ html, className }: { html: string; className?: string }) {
  const clean = useMemo(() => cleanHtml(html), [html]);
  return (
    <div
      className={cn(
        'text-base text-body leading-relaxed [&_a]:text-brand-600 [&_a]:underline [&_img]:max-w-full [&_img]:h-auto [&_img]:rounded-md [&_ul]:pl-5 [&_ol]:pl-5',
        className,
      )}
      // HTML đã lọc ở server (D13) và lọc lại ở trên
      dangerouslySetInnerHTML={{ __html: clean }}
    />
  );
}

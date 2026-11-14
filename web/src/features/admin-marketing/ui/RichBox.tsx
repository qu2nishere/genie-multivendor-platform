// Ô soạn nội dung thông báo (prototype PlatformNotifications.jsx RichBox): In đậm, In nghiêng, Liên kết
// (chỉ http/https). `sanitizeHtml` giữ đúng các thẻ ô soạn tạo ra (p, br, b/strong, i/em, a, ul/ol/li,
// div) và bỏ mọi thuộc tính trừ href an toàn — dùng cả khi lưu lẫn khi hiển thị.
import { useEffect, useRef, useState } from 'react';
import { useT } from '@/shared/hooks/useT';
import { Button, Input, Modal } from '@/shared/ui';

const ALLOWED = new Set(['P', 'BR', 'B', 'STRONG', 'I', 'EM', 'A', 'UL', 'OL', 'LI', 'DIV']);

export function sanitizeHtml(html: string): string {
  if (typeof DOMParser === 'undefined') return html.replace(/<[^>]*>/g, '');
  const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html');
  const root = doc.body.firstElementChild;
  if (!root) return '';
  const walk = (el: Element) => {
    for (const child of Array.from(el.children)) {
      walk(child);
      if (!ALLOWED.has(child.tagName)) {
        child.replaceWith(...Array.from(child.childNodes));
        continue;
      }
      const href = child.tagName === 'A' ? child.getAttribute('href') : null;
      for (const a of Array.from(child.attributes)) child.removeAttribute(a.name);
      if (href && /^https?:\/\//i.test(href)) {
        child.setAttribute('href', href);
        child.setAttribute('target', '_blank');
        child.setAttribute('rel', 'noopener noreferrer');
      }
    }
  };
  walk(root);
  return root.innerHTML;
}

const btn =
  'min-w-8 h-[30px] px-2 border border-solid border-line rounded-sm bg-surface-card cursor-pointer text-[13px] font-semibold text-body hover:bg-gray-50 focus-visible:outline-none focus-visible:shadow-focus';

export function RichBox({
  initial,
  onChange,
  label,
  invalid,
}: {
  initial: string;
  onChange: (html: string) => void;
  label: string;
  invalid?: boolean;
}) {
  const { t } = useT();
  const ref = useRef<HTMLDivElement>(null);
  const [asking, setAsking] = useState(false);
  const [url, setUrl] = useState('https://');
  const saved = useRef<Range | null>(null);
  useEffect(() => {
    if (ref.current) ref.current.innerHTML = sanitizeHtml(initial);
    // Chỉ nạp nội dung ban đầu một lần (ô soạn tự quản lý DOM)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  const emit = () => ref.current && onChange(sanitizeHtml(ref.current.innerHTML));
  const cmd = (name: string, arg?: string) => {
    ref.current?.focus();
    if (saved.current) {
      const sel = window.getSelection();
      sel?.removeAllRanges();
      sel?.addRange(saved.current);
      saved.current = null;
    }
    document.execCommand(name, false, arg);
    emit();
  };
  return (
    <div
      className={`mt-1.5 border-[1.5px] border-solid rounded-md overflow-hidden ${invalid ? 'border-red-500' : 'border-line'}`}
    >
      <div className="flex gap-1.5 p-1.5 bg-gray-25 border-0 border-b border-solid border-line-subtle">
        <button
          type="button"
          className={btn}
          aria-label={t('admin-marketing.notif.rich.bold')}
          onClick={() => cmd('bold')}
        >
          <b>B</b>
        </button>
        <button
          type="button"
          className={btn}
          aria-label={t('admin-marketing.notif.rich.italic')}
          onClick={() => cmd('italic')}
        >
          <i>I</i>
        </button>
        <button
          type="button"
          className={btn}
          onClick={() => {
            const sel = window.getSelection();
            saved.current = sel && sel.rangeCount ? sel.getRangeAt(0).cloneRange() : null;
            setUrl('https://');
            setAsking(true);
          }}
        >
          {t('admin-marketing.notif.rich.link')}
        </button>
      </div>
      <div
        ref={ref}
        role="textbox"
        aria-multiline="true"
        aria-label={label}
        contentEditable
        suppressContentEditableWarning
        onInput={emit}
        className="min-h-24 px-3 py-[9px] text-base text-body outline-none [&_a]:text-link"
      />
      <Modal
        open={asking}
        onClose={() => setAsking(false)}
        width={420}
        title={t('admin-marketing.notif.rich.link_title')}
        footer={
          <>
            <Button variant="ghost" onClick={() => setAsking(false)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              disabled={!/^https?:\/\/\S+\.\S+/.test(url)}
              onClick={() => {
                setAsking(false);
                cmd('createLink', url);
              }}
            >
              {t('admin-marketing.notif.rich.insert')}
            </Button>
          </>
        }
      >
        <Input
          label="URL"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://"
        />
      </Modal>
    </div>
  );
}

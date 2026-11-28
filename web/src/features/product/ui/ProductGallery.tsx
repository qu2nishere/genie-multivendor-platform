// Thư viện ảnh sản phẩm + Lightbox (prototype ProductDetail.jsx .tr-gallery, Lightbox) — plan G08.24.
// Ảnh chính SSR với fetchpriority=high (G08.51); bấm ảnh mở lightbox: ←/→ đổi ảnh, Esc đóng,
// bẫy focus, trả focus về ảnh chính. Island client:idle. Chọn phân loại có ảnh (vd màu Đỏ) ở khối mua → ảnh lớn
// đổi sang ảnh của phân loại đó (nghe $selection); bấm một ảnh nhỏ thì quay về thư viện ảnh.
import { useStore } from '@nanostores/react';
import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useFocusTrap } from '@/shared/hooks/useFocusTrap';
import { useScrollLock } from '@/shared/hooks/useScrollLock';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon } from '@/shared/ui';
import { $selection } from '../application/selection.store';
import { selectedImage } from '../domain/product.rules';
import type { VariantGroup } from '../domain/product.types';

export interface ProductGalleryProps {
  images: string[];
  title: string;
  /** Nhóm phân loại (ảnh theo lựa chọn) — có thì ảnh lớn đổi theo lựa chọn ở khối mua. */
  variantGroups?: VariantGroup[];
}

const LB_BTN =
  'grid h-[46px] w-[46px] flex-none cursor-pointer place-items-center rounded-full border-0 bg-white/[.14] text-white hover:bg-white/25 focus-visible:outline-none focus-visible:shadow-focus';

function Lightbox({
  images,
  start,
  title,
  onClose,
}: {
  images: string[];
  start: number;
  title: string;
  onClose: () => void;
}) {
  const { t } = useT();
  const [i, setI] = useState(start);
  const box = useRef<HTMLDivElement>(null);
  const close = useRef<HTMLButtonElement>(null);
  const step = (d: number) => setI((x) => (x + d + images.length) % images.length);
  useFocusTrap(box, true, close);
  useScrollLock(true);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      else if (e.key === 'ArrowLeft') step(-1);
      else if (e.key === 'ArrowRight') step(1);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [onClose]);

  return createPortal(
    <div
      ref={box}
      role="dialog"
      aria-modal="true"
      aria-label={t('product.gallery.lightbox', { title })}
      onClick={onClose}
      className="fixed inset-0 z-[5000] flex items-center justify-center gap-4 bg-[rgba(20,23,31,.88)] p-4 max-md:gap-1"
    >
      {images.length > 1 && (
        <button
          type="button"
          aria-label={t('product.gallery.prev')}
          className={LB_BTN}
          onClick={(e) => {
            e.stopPropagation();
            step(-1);
          }}
        >
          <Icon name="chevron-left" size={26} />
        </button>
      )}
      <img
        src={images[i]}
        alt={t('product.gallery.image_of', { title, n: i + 1, total: images.length })}
        width={900}
        height={900}
        onClick={(e) => e.stopPropagation()}
        className="h-auto max-h-[84vh] w-auto max-w-[76vw] rounded-lg bg-white object-contain max-md:max-w-[72vw]"
      />
      {images.length > 1 && (
        <button
          type="button"
          aria-label={t('product.gallery.next')}
          className={LB_BTN}
          onClick={(e) => {
            e.stopPropagation();
            step(1);
          }}
        >
          <Icon name="chevron-right" size={26} />
        </button>
      )}
      <button
        ref={close}
        type="button"
        aria-label={t('common.action.close')}
        onClick={onClose}
        className={cn(LB_BTN, 'absolute right-[18px] top-[18px]')}
      >
        <Icon name="x" size={22} />
      </button>
      <p className="sr-only" aria-live="polite">
        {t('product.gallery.position', { n: i + 1, total: images.length })}
      </p>
    </div>,
    document.body,
  );
}

export default function ProductGallery({ images, title, variantGroups = [] }: ProductGalleryProps) {
  const { t } = useT();
  const [active, setActive] = useState(0);
  const [open, setOpen] = useState(false);
  const main = useRef<HTMLButtonElement>(null);
  const sel = useStore($selection);
  const picked = selectedImage(variantGroups, sel);
  // bấm ảnh nhỏ khi đang xem ảnh phân loại → ẩn ảnh phân loại đó cho tới khi đổi lựa chọn
  const [dismissed, setDismissed] = useState<string | null>(null);
  // đổi lựa chọn (kể cả chọn lại đúng màu cũ) → hiện lại ảnh phân loại
  useEffect(() => setDismissed(null), [sel]);
  const variantImg = picked && picked !== dismissed ? picked : null;
  const shown = variantImg ? [variantImg, ...images] : images;
  const current = variantImg ?? images[active];

  return (
    <>
      <button
        ref={main}
        type="button"
        className="tr-gallery block aspect-square w-full cursor-zoom-in overflow-hidden rounded-md border-0 bg-gray-100 p-0"
        onClick={() => shown.length > 0 && setOpen(true)}
        aria-label={t('product.gallery.zoom', { title })}
      >
        {current ? (
          <img
            src={current}
            alt={title}
            width={440}
            height={440}
            fetchPriority={active === 0 && !variantImg ? 'high' : undefined}
            className="h-full w-full object-cover"
          />
        ) : (
          // Chưa có ảnh — khung giữ chỗ thay cho <img src="">
          <span
            role="img"
            aria-label={title}
            className="flex h-full w-full items-center justify-center text-subtle"
          >
            <Icon name="image" size={56} />
          </span>
        )}
      </button>
      <div className="mt-2.5 flex gap-2" role="group" aria-label={t('product.gallery.thumbs')}>
        {images.map((src, i) => (
          <button
            key={`${src}-${i}`}
            type="button"
            aria-label={t('product.gallery.thumb', { n: i + 1 })}
            aria-pressed={!variantImg && i === active}
            onClick={() => {
              setActive(i);
              if (variantImg) setDismissed(variantImg);
            }}
            className={cn(
              'h-16 w-16 cursor-pointer overflow-hidden rounded-sm border-2 border-solid bg-transparent p-0',
              !variantImg && i === active
                ? 'border-primary'
                : 'border-transparent hover:border-line-strong',
            )}
          >
            <img
              src={src}
              alt=""
              width={64}
              height={64}
              loading="lazy"
              className="h-full w-full object-cover"
            />
          </button>
        ))}
      </div>
      {open && (
        <Lightbox
          images={shown}
          start={variantImg ? 0 : active}
          title={title}
          onClose={() => {
            setOpen(false);
            main.current?.focus();
          }}
        />
      )}
    </>
  );
}

// Banner do Admin quản lý (prototype Shared.jsx Banner, BannerCarousel, BannerSlot, BannerPair) — G08.08.
// Mỗi banner là <a href> thật (R16). Carousel: 6 giây / slide, chấm vàng chạy theo thời gian,
// dừng khi rê chuột / focus, vuốt để chuyển, đứng yên khi prefers-reduced-motion.
import { useEffect, useRef, useState, type PointerEvent as RPointerEvent } from 'react';
import { useMediaQuery, MQ } from '@/shared/hooks/useMediaQuery';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon } from '@/shared/ui';
import { dealTwo, SLIDE_MS } from '../../domain/banner.rules';
import type { Banner } from '../../domain/banner.types';
import { trackBannerClick, trackBannerView } from '../../application/bannerTracking';

/** Ảnh banner: màn ≤768px dùng ảnh mobile nếu Admin đã tải (cả hai cắt từ giữa). */
export function BannerPicture({ b, priority }: { b: Banner; priority?: boolean }) {
  return (
    <picture>
      {b.imageMobile && <source media="(max-width: 768px)" srcSet={b.imageMobile} />}
      <img
        src={b.image}
        alt={b.title}
        width={1600}
        height={400}
        loading={priority ? 'eager' : 'lazy'}
        fetchPriority={priority ? 'high' : undefined}
        decoding="async"
      />
    </picture>
  );
}

export function BannerLink({
  b,
  priority,
  ...rest
}: {
  b: Banner;
  priority?: boolean;
  tabIndex?: number;
  'aria-hidden'?: boolean;
  'aria-label'?: string;
}) {
  useEffect(() => {
    trackBannerView(b.id);
  }, [b.id]);
  return (
    <a href={b.href} className="tr-banner" onClick={() => trackBannerClick(b.id)} {...rest}>
      <BannerPicture b={b} priority={priority} />
    </a>
  );
}

export function BannerCarousel({
  items,
  compact,
  delay = 0,
  priority,
}: {
  items: Banner[];
  compact?: boolean;
  delay?: number;
  priority?: boolean;
}) {
  const { t } = useT();
  const [i, setI] = useState(0);
  const [hold, setHold] = useState(false);
  const still = useMediaQuery(MQ.reducedMotion);
  const drag = useRef<{ x: number; moved: boolean } | null>(null);
  const first = useRef(true);
  const n = items.length;
  const wait = SLIDE_MS + (first.current ? delay : 0);

  useEffect(() => {
    if (n < 2 || hold || still) return undefined;
    const h = setTimeout(() => {
      first.current = false;
      setI((x) => (x + 1) % n);
    }, wait);
    return () => clearTimeout(h);
  }, [n, hold, i, still, wait]);

  if (!n) return null;
  const cur = i % n;
  const step = (d: number) => setI((x) => (x + d + n) % n);
  const down = (e: RPointerEvent) => {
    drag.current = { x: e.clientX, moved: false };
  };
  const up = (e: RPointerEvent) => {
    if (!drag.current) return;
    const dx = e.clientX - drag.current.x;
    if (Math.abs(dx) > 40 && n > 1) {
      drag.current.moved = true;
      step(dx < 0 ? 1 : -1);
    }
  };

  return (
    <div
      className={cn('tr-carousel', compact && 'tr-carousel--compact')}
      role="region"
      aria-roledescription={t('promotions.banner.carousel')}
      aria-label={t('promotions.banner.label')}
      data-hold={hold ? 1 : 0}
      onMouseEnter={() => setHold(true)}
      onMouseLeave={() => setHold(false)}
      onFocus={() => setHold(true)}
      onBlur={() => setHold(false)}
      onPointerDown={down}
      onPointerUp={up}
      onClickCapture={(e) => {
        // Kéo đã đổi slide thì không tính là bấm vào banner
        if (drag.current?.moved) {
          e.preventDefault();
          e.stopPropagation();
        }
        drag.current = null;
      }}
    >
      <div className="tr-carousel__track" style={{ transform: `translateX(${-100 * cur}%)` }}>
        {items.map((x, k) => (
          <BannerLink
            key={x.id}
            b={x}
            priority={priority && k === 0}
            tabIndex={k === cur ? 0 : -1}
            aria-hidden={k !== cur}
            aria-label={`${x.title} (${k + 1}/${n})`}
          />
        ))}
      </div>
      {n > 1 && (
        <>
          {!compact && (
            <>
              <button
                type="button"
                className="tr-carousel__nav tr-carousel__nav--prev"
                aria-label={t('promotions.banner.prev')}
                onClick={() => step(-1)}
              >
                <Icon name="chevron-left" size={20} />
              </button>
              <button
                type="button"
                className="tr-carousel__nav tr-carousel__nav--next"
                aria-label={t('promotions.banner.next')}
                onClick={() => step(1)}
              >
                <Icon name="chevron-right" size={20} />
              </button>
            </>
          )}
          <div className="tr-carousel__dots">
            {items.map((x, k) => (
              <button
                key={x.id}
                type="button"
                aria-label={t('promotions.banner.go', { n: k + 1 })}
                aria-current={k === cur ? 'true' : undefined}
                onClick={() => setI(k)}
              >
                {k === cur && !still && <i key={i} style={{ animationDuration: `${wait}ms` }} />}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

/** Một ô: một banner thì đứng yên, nhiều banner thì xoay (carousel nhỏ, không mũi tên). */
export function BannerSlot({ items, delay }: { items: Banner[]; delay?: number }) {
  if (items.length === 1) return <BannerLink b={items[0]} />;
  return <BannerCarousel items={items} compact delay={delay} />;
}

/** Vị trí hai ô (bên phải băng đầu trang, giữa trang): chia xen kẽ, mỗi ô xoay riêng. */
export function BannerPair({ items, className }: { items: Banner[]; className: string }) {
  const slots = dealTwo(items);
  return (
    <div className={cn(className, slots.length === 1 && `${className}--solo`)}>
      {slots.map((s, k) => (
        <BannerSlot key={s[0].id} items={s} delay={(k * SLIDE_MS) / 2} />
      ))}
    </div>
  );
}

/** Đầu trang chủ: carousel 2/3 + hai banner phụ xếp dọc (prototype .tr-heroban). Island client:visible. */
export default function HomeBanners({ top, side }: { top: Banner[]; side: Banner[] }) {
  if (!top.length && !side.length) return null;
  return (
    <div className={cn('tr-heroban', !(top.length && side.length) && 'tr-heroban--solo')}>
      {top.length > 0 && <BannerCarousel items={top} priority />}
      {side.length > 0 && <BannerPair items={side} className="tr-heroban__side" />}
    </div>
  );
}

// Popup banner một lần mỗi phiên (prototype Shared.jsx BannerPopup) — plan G08.09, island client:idle.
// Các banner xếp thành chồng thẻ: thẻ trước là banner, tối đa 3 thẻ sau ló góc trái. Mũi tên trái
// chia thẻ kế tiếp lên trên, mũi tên phải lấy lại thẻ vừa chia. Esc / nền / nút đóng để tắt.
import { useEffect, useRef, useState, type CSSProperties } from 'react';
import { useFocusTrap } from '@/shared/hooks/useFocusTrap';
import { useT } from '@/shared/hooks/useT';
import { store } from '@/shared/lib/storage';
import { Icon } from '@/shared/ui';
import type { Banner } from '../../domain/banner.types';
import { BannerLink, BannerPicture } from './Banners';

const DECK_SHOWN = 4;

export default function BannerPopup({ items }: { items: Banner[] }) {
  const { t } = useT();
  const [open, setOpen] = useState(false);
  const [order, setOrder] = useState(() => items.map((_, k) => k));
  const [flying, setFlying] = useState<number | null>(null);
  const deck = useRef<HTMLDivElement>(null);

  // Chỉ mở sau khi hydrate và chưa xem trong phiên này (sessionStorage genie.popup.seen)
  useEffect(() => {
    if (items.length && !store.get('popupSeen')) setOpen(true);
  }, [items.length]);
  useFocusTrap(deck, open);

  const close = () => {
    setOpen(false);
    store.set('popupSeen', '1');
  };
  const bring = (k: number) => {
    if (order[0] === k) return;
    setFlying(k);
    setOrder(items.map((_, i) => (k + i) % items.length));
    setTimeout(() => setFlying(null), 520);
  };
  const next = () => bring((order[0] + 1) % items.length);
  const back = () => bring((order[0] - 1 + items.length) % items.length);

  useEffect(() => {
    if (!open) return undefined;
    const key = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close();
      if (e.key === 'ArrowLeft' && items.length > 1) next();
      if (e.key === 'ArrowRight' && items.length > 1) back();
    };
    window.addEventListener('keydown', key);
    return () => window.removeEventListener('keydown', key);
  });

  if (!open || !items.length) return null;
  const front = items[order[0]];
  return (
    <div className="tr-popup" onClick={close}>
      <div
        ref={deck}
        className="tr-deck"
        role="dialog"
        aria-modal="true"
        aria-label={
          items.length > 1
            ? t('promotions.popup.label_many', { title: front.title, count: items.length })
            : front.title
        }
        onClick={(e) => e.stopPropagation()}
      >
        <button type="button" className="tr-deck__close" onClick={close} aria-label={t('ui.close')}>
          <Icon name="x" size={20} />
        </button>
        <div className="tr-deck__stack">
          {order
            .slice(0, DECK_SHOWN)
            .reverse()
            .map((k) => {
              const depth = order.indexOf(k);
              const b = items[k];
              return (
                <div
                  key={b.id}
                  className="tr-deck__card"
                  data-depth={depth}
                  data-fly={flying === k ? 1 : 0}
                  style={{ '--d': depth } as CSSProperties}
                >
                  {depth === 0 ? (
                    <BannerLink b={b} />
                  ) : (
                    <div className="tr-deck__peek" aria-hidden="true">
                      <BannerPicture b={b} />
                    </div>
                  )}
                </div>
              );
            })}
        </div>
        {items.length > 1 && (
          <>
            <button
              type="button"
              className="tr-deck__arrow tr-deck__arrow--next"
              onClick={next}
              aria-label={t('promotions.popup.next')}
            >
              <Icon name="chevron-left" size={24} />
            </button>
            <button
              type="button"
              className="tr-deck__arrow tr-deck__arrow--back"
              onClick={back}
              aria-label={t('promotions.popup.back')}
            >
              <Icon name="chevron-right" size={24} />
            </button>
            <div className="tr-deck__nav">
              {items
                .map((b, k) => ({ b, k }))
                .reverse()
                .map(({ b, k }) => (
                  <button
                    key={b.id}
                    type="button"
                    aria-label={t('promotions.popup.go', { n: k + 1, title: b.title })}
                    aria-current={order[0] === k ? 'true' : undefined}
                    onClick={() => bring(k)}
                  />
                ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

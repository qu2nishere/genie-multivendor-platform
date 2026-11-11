import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { buttonClass, Icon } from '@/shared/ui';
import type { CartLine } from '../domain/cart.types';

const productHref = (l: CartLine) => `/p/${l.productSlug}-${l.productId}`;

/**
 * Giỏ rút gọn dưới biểu tượng giỏ ở header (prototype Header.jsx MiniCart) — plan G07.15:
 * 5 dòng mới thêm, giá, "Xem Giỏ Hàng"; giỏ trống có minh họa.
 */
export function MiniCart({ lines, onNavigate }: { lines: CartLine[]; onNavigate?: () => void }) {
  const { t, locale } = useT();
  const recent = [...lines].reverse().slice(0, 5);
  if (lines.length === 0) {
    return (
      <div className="w-[360px] max-w-[calc(100vw-16px)] px-4 py-10 text-center">
        <span className="w-[72px] h-[72px] rounded-full bg-gray-50 text-muted grid place-items-center mx-auto mb-4">
          <Icon name="shopping-cart" size={36} />
        </span>
        <p className="m-0 mb-1.5 font-semibold text-base text-strong">
          {t('cart.mini.empty_title')}
        </p>
        <p className="m-0 text-sm text-muted">{t('cart.mini.empty_text')}</p>
      </div>
    );
  }
  return (
    <div className="w-[360px] max-w-[calc(100vw-16px)] overflow-hidden rounded-lg text-body">
      <p className="m-0 px-4 py-3 text-sm text-muted border-b border-line-subtle">
        {t('cart.mini.recent')}
      </p>
      <ul className="max-h-[360px] overflow-y-auto list-none m-0 p-0">
        {recent.map((l) => (
          <li key={l.key}>
            <a
              href={productHref(l)}
              onClick={onNavigate}
              className="flex gap-3 px-4 py-3 border-b border-line-subtle no-underline hover:no-underline hover:bg-gray-50 text-[inherit]"
            >
              <img
                src={l.image}
                alt=""
                width={48}
                height={48}
                loading="lazy"
                className="w-12 h-12 object-cover rounded-sm border border-line flex-none"
              />
              <span className="flex-1 min-w-0 flex flex-col justify-center">
                <span className="font-medium text-sm text-strong truncate mb-1">{l.title}</span>
                <span className="flex items-center justify-between gap-2">
                  <span className="text-xs text-subtle truncate">{l.variantLabel}</span>
                  <span className="font-semibold text-sm text-primary flex-none">
                    {formatVND(l.price, { locale })}
                  </span>
                </span>
              </span>
            </a>
          </li>
        ))}
      </ul>
      <div className="flex items-center justify-between gap-3 px-4 py-3 bg-gray-50">
        <span className="text-sm text-muted">
          {lines.length > 5 ? t('cart.mini.more', { count: lines.length - 5 }) : ''}
        </span>
        <a
          href="/cart"
          onClick={onNavigate}
          className={buttonClass({ size: 'sm', className: 'px-5' })}
        >
          {t('cart.mini.view')}
        </a>
      </div>
    </div>
  );
}

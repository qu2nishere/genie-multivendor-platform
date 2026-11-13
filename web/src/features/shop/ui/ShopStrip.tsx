// Dải shop trên trang sản phẩm (prototype ProductDetail.jsx .tr-vstrip) — plan G08.28, island client:idle.
// Con dấu + tên shop (liên kết thật), Chính hãng / Shop Yêu Thích, Online / Đang tạm nghỉ,
// Theo dõi (bỏ theo dõi cần xác nhận; khách → nhắc đăng nhập), Chat ngay, Xem shop.
import { useT } from '@/shared/hooks/useT';
import { formatCompact, formatNumber } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import { Badge, Button, Seal } from '@/shared/ui';
import { shopHref } from '../domain/shop.rules';
import type { Shop } from '../domain/shop.types';
import { ChatButton, FollowButton } from './FollowButton';

export interface ShopStripProps {
  shop: Pick<
    Shop,
    'id' | 'name' | 'authentic' | 'favorite' | 'followerCount' | 'location' | 'rating' | 'vacation'
  >;
  /** Sản phẩm đang xem — mở chat kèm thẻ sản phẩm. */
  productId: number;
}

function Strip({ shop, productId }: ShopStripProps) {
  const { t, locale } = useT();
  const href = shopHref(shop.id);

  return (
    <section
      className="tr-vstrip my-4 rounded-lg bg-surface-card px-5 py-4 shadow-sm"
      aria-label={t('shop.strip.label', { shop: shop.name })}
    >
      <a
        href={href}
        className="flex-none rounded-full"
        aria-label={t('shop.strip.visit', { shop: shop.name })}
      >
        <Seal id={shop.id} name={shop.name} size={56} />
      </a>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-1.5">
          <a href={href} className="text-lg font-bold text-strong no-underline hover:underline">
            {shop.name}
          </a>
          {shop.authentic && <Badge variant="primary">{t('product.badge.authentic')}</Badge>}
          {shop.favorite && <Badge variant="primary">{t('shop.badge.favorite')}</Badge>}
          <span className={shop.vacation ? 'text-xs text-muted' : 'text-xs text-mint-600'}>
            {shop.vacation ? t('shop.state.vacation') : t('shop.state.online')}
          </span>
        </div>
        <div className="mt-[3px] text-xs text-muted">
          {t('shop.strip.meta', {
            followers: formatCompact(shop.followerCount, { locale }),
            location: shop.location,
            rating: formatNumber(shop.rating, { locale }),
          })}
        </div>
      </div>
      <div className="tr-vstrip__act">
        <FollowButton shopId={shop.id} shopName={shop.name} />
        <ChatButton shopId={shop.id} productId={productId} />
        <Button variant="ghost" iconLeft="store" href={href}>
          {t('shop.strip.view')}
        </Button>
      </div>
    </section>
  );
}

export default function ShopStrip(props: ShopStripProps) {
  return (
    <Providers>
      <Strip {...props} />
    </Providers>
  );
}

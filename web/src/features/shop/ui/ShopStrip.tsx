// Dải shop trên trang sản phẩm (prototype ProductDetail.jsx .tr-vstrip) — plan G08.28, island client:idle.
// Con dấu + tên shop (liên kết thật), Chính hãng / Shop Yêu Thích, Online / Đang tạm nghỉ,
// Theo dõi (bỏ theo dõi cần xác nhận; khách → nhắc đăng nhập), Chat ngay, Xem shop.
import { useStore } from '@nanostores/react';
import { useState } from 'react';
import { requireLogin } from '@/features/auth';
import { errorMessage } from '@/shared/api/errors';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { formatCompact, formatNumber } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import { Badge, Button, ConfirmDialog, Seal, toast } from '@/shared/ui';
import { useFollowedShops, useSetFollow } from '../application/useFollow';
import { shopHref } from '../domain/shop.rules';
import type { Shop } from '../domain/shop.types';

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
  const session = useStore($session);
  const authed = session.status === 'authenticated';
  const followed = useFollowedShops(authed);
  const mutate = useSetFollow();
  const [confirm, setConfirm] = useState(false);
  const following = authed && !!followed.data?.some((s) => s.id === shop.id);
  const href = shopHref(shop.id);

  const set = (on: boolean) =>
    mutate.mutate(
      { shopId: shop.id, on },
      {
        onSuccess: () => {
          setConfirm(false);
          toast.info(
            on
              ? t('shop.follow.done', { shop: shop.name })
              : t('shop.follow.undone', { shop: shop.name }),
          );
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );

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
        <Button
          variant={following ? 'subtle' : 'secondary'}
          iconLeft={following ? 'check' : 'plus'}
          loading={mutate.isPending && !confirm}
          aria-pressed={following}
          onClick={() => {
            if (!authed) requireLogin();
            else if (following) setConfirm(true);
            else set(true);
          }}
        >
          {following ? t('common.action.following') : t('common.action.follow')}
        </Button>
        <Button
          variant="ghost"
          iconLeft="message-circle"
          href={`/chat?shop=${shop.id}&product=${productId}`}
          onClick={(e) => {
            if (!authed) {
              e.preventDefault();
              requireLogin(`/chat?shop=${shop.id}&product=${productId}`);
            }
          }}
        >
          {t('shop.strip.chat')}
        </Button>
        <Button variant="ghost" iconLeft="store" href={href}>
          {t('shop.strip.view')}
        </Button>
      </div>
      <ConfirmDialog
        open={confirm}
        tone="danger"
        title={t('shop.follow.confirm_title')}
        message={t('shop.follow.confirm_text', { shop: shop.name })}
        confirmLabel={t('shop.follow.confirm')}
        cancelLabel={t('shop.follow.cancel')}
        loading={mutate.isPending}
        onConfirm={() => set(false)}
        onCancel={() => setConfirm(false)}
      />
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

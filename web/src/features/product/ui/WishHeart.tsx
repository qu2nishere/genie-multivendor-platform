import { useStore } from '@nanostores/react';
import { useEffect } from 'react';
import { requireLogin } from '@/features/auth';
import { errorMessage } from '@/shared/api/errors';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { HeartToggle, toast } from '@/shared/ui';
import { $wishlist, loadWishlist, toggleWishlist } from '../application/wishlist.store';

/** Tim yêu thích trên thẻ sản phẩm (prototype CardX heart) — khách bấm → nhắc đăng nhập. */
export function WishHeart({ productId }: { productId: number }) {
  const { t } = useT();
  const session = useStore($session);
  const list = useStore($wishlist);
  const authed = session.status === 'authenticated';
  useEffect(() => {
    if (authed) void loadWishlist();
  }, [authed]);
  const liked = authed && list.has(productId);
  return (
    <HeartToggle
      liked={liked}
      onToggle={async () => {
        if (!authed) {
          requireLogin();
          return;
        }
        try {
          const on = await toggleWishlist(productId);
          toast.info(on ? t('product.wishlist.added') : t('product.wishlist.removed'));
        } catch (e) {
          toast.error(errorMessage(e, t));
        }
      }}
    />
  );
}

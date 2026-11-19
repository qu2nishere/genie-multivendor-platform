// Nút "Yêu thích" + "Chia sẻ" dưới thư viện ảnh (prototype ProductDetail.jsx toggleLike, share) —
// plan G08.27, island client:idle. Chia sẻ (UC-CAT-18; plan 2026-11-17 C07.05): liên kết chuẩn
// /p/{slug}-{id}?v={variant_id}&src=share (v chỉ có khi đã chọn đủ phân loại), dùng Web Share API khi có,
// không thì Clipboard API.
import { useStore } from '@nanostores/react';
import { useEffect } from 'react';
import { requireLogin } from '@/features/auth';
import { errorMessage } from '@/shared/api/errors';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Icon, toast } from '@/shared/ui';
import { $selection } from '../application/selection.store';
import { $wishlist, loadWishlist, toggleWishlist } from '../application/wishlist.store';
import { shareVariantParam } from '../domain/product.rules';
import type { ProductSku, VariantGroup } from '../domain/product.types';

export interface WishShareProps {
  productId: number;
  title: string;
  /** Đường dẫn chuẩn /p/{slug}-{id}. */
  href: string;
  variantGroups: VariantGroup[];
  /** SKU để đổi lựa chọn thành `variant_id` trong liên kết. */
  skus: ProductSku[];
}

const BTN =
  'inline-flex min-h-[44px] cursor-pointer items-center gap-1.5 border-0 bg-transparent px-0 text-sm hover:text-primary';

export default function WishShare({ productId, title, href, variantGroups, skus }: WishShareProps) {
  const { t } = useT();
  const session = useStore($session);
  const list = useStore($wishlist);
  const sel = useStore($selection);
  const authed = session.status === 'authenticated';
  const liked = authed && list.has(productId);

  useEffect(() => {
    if (authed) void loadWishlist();
  }, [authed]);

  const like = async () => {
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
  };

  const share = async () => {
    const u = new URL(href, window.location.origin);
    const variantId = shareVariantParam({ variantGroups, skus }, sel);
    if (variantId) u.searchParams.set('v', variantId);
    u.searchParams.set('src', 'share');
    if (navigator.share) {
      try {
        await navigator.share({ title, url: u.href });
      } catch {
        // người dùng huỷ chia sẻ
      }
      return;
    }
    try {
      await navigator.clipboard.writeText(u.href);
      toast.success(t('product.share.copied'));
    } catch {
      toast.error(t('product.share.failed'));
    }
  };

  return (
    <div className="mt-3.5 flex gap-2.5">
      <button
        type="button"
        aria-pressed={liked}
        onClick={() => void like()}
        className={cn(BTN, liked ? 'text-primary' : 'text-muted')}
      >
        <Icon name="heart" size={16} style={{ fill: liked ? 'currentColor' : 'none' }} />
        {liked ? t('product.wishlist.on') : t('product.wishlist.off')}
      </button>
      <button type="button" onClick={() => void share()} className={cn(BTN, 'text-muted')}>
        <Icon name="share-2" size={16} />
        {t('common.action.share')}
      </button>
    </div>
  );
}

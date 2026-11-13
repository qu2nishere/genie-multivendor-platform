// Nút Theo dõi / Đang theo dõi (prototype ProductDetail.jsx, Shop.jsx) — bỏ theo dõi cần xác nhận,
// khách → nhắc đăng nhập. Cần nằm trong <Providers> (TanStack Query).
import { useStore } from '@nanostores/react';
import { useState } from 'react';
import { requireLogin } from '@/features/auth';
import { errorMessage } from '@/shared/api/errors';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { Button, ConfirmDialog, toast, type ButtonVariant } from '@/shared/ui';
import { useFollowedShops, useSetFollow } from '../application/useFollow';

export interface FollowButtonProps {
  shopId: number;
  shopName: string;
  /** Kiểu nút khi đang theo dõi (trang sản phẩm: subtle; trang shop: outline). */
  followingVariant?: ButtonVariant;
}

export function FollowButton({ shopId, shopName, followingVariant = 'subtle' }: FollowButtonProps) {
  const { t } = useT();
  const session = useStore($session);
  const authed = session.status === 'authenticated';
  const followed = useFollowedShops(authed);
  const mutate = useSetFollow();
  const [confirm, setConfirm] = useState(false);
  const following = authed && !!followed.data?.some((s) => s.id === shopId);

  const set = (on: boolean) =>
    mutate.mutate(
      { shopId, on },
      {
        onSuccess: () => {
          setConfirm(false);
          toast.info(
            on
              ? t('shop.follow.done', { shop: shopName })
              : t('shop.follow.undone', { shop: shopName }),
          );
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );

  return (
    <>
      <Button
        variant={following ? followingVariant : 'secondary'}
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
      <ConfirmDialog
        open={confirm}
        tone="danger"
        title={t('shop.follow.confirm_title')}
        message={t('shop.follow.confirm_text', { shop: shopName })}
        confirmLabel={t('shop.follow.confirm')}
        cancelLabel={t('shop.follow.cancel')}
        loading={mutate.isPending}
        onConfirm={() => set(false)}
        onCancel={() => setConfirm(false)}
      />
    </>
  );
}

/** Liên kết "Chat ngay" — khách bị nhắc đăng nhập (trang /chat cần đăng nhập). */
export function ChatButton({
  shopId,
  productId,
  variant = 'ghost',
}: {
  shopId: number;
  productId?: number;
  variant?: ButtonVariant;
}) {
  const { t } = useT();
  const session = useStore($session);
  const href = `/chat?shop=${shopId}${productId ? `&product=${productId}` : ''}`;
  return (
    <Button
      variant={variant}
      iconLeft="message-circle"
      href={href}
      onClick={(e) => {
        if (session.status !== 'authenticated') {
          e.preventDefault();
          requireLogin(href);
        }
      }}
    >
      {t('shop.strip.chat')}
    </Button>
  );
}

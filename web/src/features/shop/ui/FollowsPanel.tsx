// "Shop đang theo dõi" (prototype Account.jsx FollowsPanel) — plan G10.59: thẻ shop (con dấu, Chính hãng,
// đánh giá, người theo dõi), Vào shop, bỏ theo dõi có xác nhận.
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatCompact } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import {
  Badge,
  Button,
  ConfirmDialog,
  EmptyState,
  Icon,
  PanelHead,
  Seal,
  Skeleton,
  toast,
} from '@/shared/ui';
import { useFollowedShops, useSetFollow } from '../application/useFollow';
import { shopHref } from '../domain/shop.rules';
import type { Shop } from '../domain/shop.types';

function Panel() {
  const { t, locale } = useT();
  const q = useFollowedShops(true);
  const set = useSetFollow();
  const [unfollow, setUnfollow] = useState<Shop | null>(null);
  const list = q.data ?? [];
  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm px-[22px] py-5 max-sm:px-4"
      aria-labelledby="follow-title"
    >
      <PanelHead
        id="follow-title"
        icon="store"
        title={t('shop.follows.title', { count: list.length })}
        sub={t('shop.follows.sub')}
      />
      {q.isPending && <Skeleton lines={3} className="mt-4" />}
      {!q.isPending && list.length === 0 && (
        <EmptyState icon="store" text={t('shop.follows.empty')} />
      )}
      <ul className="list-none m-0 p-0 grid grid-cols-2 gap-3.5 mt-4 max-md:grid-cols-1">
        {list.map((s) => (
          <li key={s.id} className="border border-solid border-line-subtle rounded-md px-4 py-3.5">
            <div className="flex items-center gap-3">
              <a href={shopHref(s.id)} aria-hidden="true" tabIndex={-1} className="flex-none">
                <Seal id={s.id} name={s.name} size={48} />
              </a>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1.5 flex-wrap">
                  <a
                    href={shopHref(s.id)}
                    className="font-bold text-sm text-strong no-underline hover:underline"
                  >
                    {s.name}
                  </a>
                  {s.authentic && <Badge variant="primary">{t('common.authentic')}</Badge>}
                </div>
                <div className="inline-flex items-center gap-1 text-xs text-muted mt-0.5">
                  <Icon name="star" size={12} className="text-gold-500" />
                  {s.rating} ·{' '}
                  {t('shop.follows.followers', {
                    count: formatCompact(s.followerCount, { locale }),
                  })}
                </div>
              </div>
            </div>
            <div className="flex gap-2 mt-3">
              <Button
                size="sm"
                variant="outline"
                iconLeft="check"
                className="flex-1"
                onClick={() => setUnfollow(s)}
              >
                {t('common.action.following')}
              </Button>
              <Button
                size="sm"
                variant="ghost"
                iconLeft="store"
                className="flex-1"
                href={shopHref(s.id)}
              >
                {t('shop.follows.visit')}
              </Button>
            </div>
          </li>
        ))}
      </ul>
      <ConfirmDialog
        open={unfollow !== null}
        tone="danger"
        title={t('shop.follows.unfollow_title')}
        message={t('shop.follows.unfollow_text', { shop: unfollow?.name ?? '' })}
        confirmLabel={t('shop.follows.unfollow')}
        cancelLabel={t('orders.cancel.keep')}
        loading={set.isPending}
        onCancel={() => setUnfollow(null)}
        onConfirm={() =>
          unfollow &&
          set.mutate(
            { shopId: unfollow.id, on: false },
            { onSuccess: () => setUnfollow(null), onError: (e) => toast.error(errorMessage(e, t)) },
          )
        }
      />
    </section>
  );
}

export default function FollowsPanel() {
  return (
    <Providers>
      <Panel />
    </Providers>
  );
}

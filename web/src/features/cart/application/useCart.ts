// Hook đọc giỏ cho trang giỏ / checkout (plan G10.03): đã đăng nhập → giỏ tài khoản từ store
// dùng chung; khách → dòng hiển thị dựng từ giỏ khách (TanStack Query tra SKU).
import { useStore } from '@nanostores/react';
import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { $session } from '@/shared/auth/session';
import type { CartLine } from '../domain/cart.types';
import { resolveGuestItems } from '../infrastructure/cart.api';
import { $cartLines, $cartStatus, $guestItems, loadCart, loadGuestCart } from './cart.store';

export interface CartView {
  /** Phiên chưa rõ (đang khởi động) — chưa biết khách hay đã đăng nhập. */
  pending: boolean;
  guest: boolean;
  lines: CartLine[];
  loading: boolean;
  error: boolean;
  reload: () => void;
}

export function useCart(): CartView {
  const session = useStore($session);
  const lines = useStore($cartLines);
  const status = useStore($cartStatus);
  const guestItems = useStore($guestItems);
  const guest = session.status === 'guest';
  const authed = session.status === 'authenticated';

  useEffect(() => {
    if (authed) void loadCart();
    if (guest) loadGuestCart();
  }, [authed, guest]);

  const guestQuery = useQuery({
    queryKey: ['cart', 'guest', guestItems.map((i) => `${i.product_variant_id}:${i.quantity}`)],
    queryFn: () => resolveGuestItems(guestItems),
    enabled: guest,
    placeholderData: (prev) => prev,
  });

  if (guest) {
    return {
      pending: false,
      guest: true,
      lines: guestQuery.data ?? [],
      loading: guestQuery.isPending && guestItems.length > 0,
      error: guestQuery.isError,
      reload: () => void guestQuery.refetch(),
    };
  }
  return {
    pending: session.status === 'unknown',
    guest: false,
    lines,
    loading: status === 'idle' || status === 'loading',
    error: status === 'error',
    reload: () => void loadCart(true),
  };
}

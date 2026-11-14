// API công khai của feature "cart" — nơi DUY NHẤT feature khác được import.
export type { CartLine, CartLineInput, GuestCart, GuestCartItem } from './domain/cart.types';
export { lineKeyOf, MAX_GUEST_LINES } from './domain/cart.rules';
export {
  $cartLines,
  $cartCount,
  $cartStatus,
  loadCart,
  setCartLines,
  $guestItems,
  loadGuestCart,
  addToCart,
  type AddResult,
} from './application/cart.store';
export { default as AddToCartButton, type AddToCartButtonProps } from './ui/AddToCartButton';
export { MiniCart } from './ui/MiniCart';
export { mergeGuestCart } from './application/mergeGuestCart';

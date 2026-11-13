// API công khai của feature "cart" — nơi DUY NHẤT feature khác được import.
export type {
  CartLine,
  CartLineInput,
  CartGroup,
  CartShop,
  GuestCart,
  GuestCartItem,
  UnavailableReason,
} from './domain/cart.types';
export {
  lineKeyOf,
  lineSubtotal,
  linesSubtotal,
  groupByShop,
  MAX_GUEST_LINES,
} from './domain/cart.rules';
export {
  $cartLines,
  $cartCount,
  $cartStatus,
  loadCart,
  setCartLines,
  $guestItems,
  loadGuestCart,
  addToCart,
  buyAgain,
  removeLines,
  type AddResult,
} from './application/cart.store';
export { useCart, type CartView } from './application/useCart';
export { mergeGuestCart } from './application/mergeGuestCart';
export { default as AddToCartButton, type AddToCartButtonProps } from './ui/AddToCartButton';
export { MiniCart } from './ui/MiniCart';
export { default as CartPage, type CartPageProps } from './ui/CartPage';
export { productHref as cartProductHref } from './ui/CartLineRow';

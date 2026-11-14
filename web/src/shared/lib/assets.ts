// Đường dẫn ảnh tĩnh trong public/img (plan G03.10) — mock không ghi cứng đường dẫn.
// Khi nối backend, ảnh sản phẩm / banner đến từ PUBLIC_S3_URL; hàm nhận cả URL đầy đủ.

const isAbsolute = (s: string) =>
  /^(https?:)?\/\//.test(s) || s.startsWith('/') || s.startsWith('data:');

const withJpg = (name: string) => (/\.[a-z0-9]+$/i.test(name) ? name : `${name}.jpg`);

/** Ảnh sản phẩm mẫu: productImg('headphones') → /img/products/headphones.jpg */
export function productImg(name: string): string {
  return isAbsolute(name) ? name : `/img/products/${withJpg(name)}`;
}

/** Ảnh banner: bannerImg('camp-1212') → /img/banners/camp-1212.jpg */
export function bannerImg(name: string): string {
  return isAbsolute(name) ? name : `/img/banners/${withJpg(name)}`;
}

/** Phương thức thanh toán trong phạm vi TS 3.5: VNPay, VietQR, COD. */
export const PAYMENT_LOGOS = {
  vnpay: '/img/payments/vnpay.png',
  card: '/img/payments/card.svg',
} as const;

/** Ảnh nền theme — chỉ nạp qua CSS khi theme tương ứng bật. */
export const THEME_ART = {
  genie: '/img/themes/genie-lamp.svg',
  dongson: '/img/themes/dongson-drum.svg',
  hoian: '/img/themes/hoian-lanterns.svg',
} as const;

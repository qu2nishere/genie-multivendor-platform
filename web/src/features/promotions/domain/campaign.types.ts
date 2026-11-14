// Chiến dịch khuyến mãi (prototype data.jsx CAMPAIGNS, Campaign.jsx) — plan G08.39.
import type { VoucherKind } from './voucher.types';

export interface Campaign {
  slug: string;
  title: string;
  /** Màu nền đầu trang (Admin chọn). */
  colour: string;
  /** Ảnh minh họa xếp chồng bên phải. */
  pictures: string[];
  intro: string;
  voucher: { code: string; kind: VoucherKind; amount: string; label: string };
  /** Sản phẩm tham gia: giảm từ N% trở lên (prototype pick: discountPct >= 45). */
  minDiscount: number;
  /** YYYY-MM-DD. */
  endsAt: string;
}

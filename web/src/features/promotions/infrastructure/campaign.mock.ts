// Chiến dịch mẫu (prototype data.jsx CAMPAIGNS).
import { productImg } from '@/shared/lib/assets';
import type { CampaignDto } from './campaign.dto';

export const CAMPAIGN_DTOS: CampaignDto[] = [
  {
    slug: 'sieu-sale-12-12',
    title: 'Siêu sale 12.12',
    colour: '#B3202E',
    pictures: ['sneakers', 'perfume', 'speaker'].map((n) => productImg(n)),
    intro:
      'Từ 00:00 ngày 12/12 các shop đã xác minh giảm sâu. Lưu mã SALE1212 hôm nay để dùng khi chiến dịch mở; Genie vẫn giữ tiền đến khi bạn nhận hàng.',
    voucher: {
      code: 'SALE1212',
      kind: 'platform',
      amount_label: '-12%',
      label: 'Đơn từ 300.000₫, dùng từ 12/12',
    },
    min_discount: 45,
    ends_at: '2026-12-12',
  },
];

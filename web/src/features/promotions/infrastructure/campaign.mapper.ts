import type { Campaign } from '../domain/campaign.types';
import type { CampaignDto } from './campaign.dto';

export const toCampaign = (d: CampaignDto): Campaign => ({
  slug: d.slug,
  title: d.title,
  colour: d.colour,
  pictures: d.pictures,
  intro: d.intro,
  voucher: {
    code: d.voucher.code,
    kind: d.voucher.kind,
    amount: d.voucher.amount_label,
    label: d.voucher.label,
  },
  minDiscount: d.min_discount,
  endsAt: d.ends_at,
});

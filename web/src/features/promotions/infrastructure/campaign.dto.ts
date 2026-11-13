export interface CampaignDto {
  slug: string;
  title: string;
  colour: string;
  pictures: string[];
  intro: string;
  voucher: {
    code: string;
    kind: 'platform' | 'shop' | 'freeship';
    amount_label: string;
    label: string;
  };
  min_discount: number;
  ends_at: string;
}

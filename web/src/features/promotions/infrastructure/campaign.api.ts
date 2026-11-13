// Chiến dịch: GET /public/campaigns/{slug}. Không có / đã kết thúc → GNE-CMP-4041 (trang trả 404).
import { request } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { env } from '@/shared/config/env';
import type { Campaign } from '../domain/campaign.types';
import type { CampaignDto } from './campaign.dto';
import { toCampaign } from './campaign.mapper';
import { CAMPAIGN_DTOS } from './campaign.mock';

export async function getCampaign(slug: string): Promise<Campaign> {
  if (USE_MOCK) {
    await mockDelay();
    const hit = CAMPAIGN_DTOS.find((c) => c.slug === slug);
    if (!hit) mockFail('GNE-CMP-4041', { status: 404, message: 'Campaign not found' });
    return toCampaign(hit);
  }
  const baseUrl =
    typeof window === 'undefined'
      ? (env.INTERNAL_API_URL ?? env.PUBLIC_API_URL)
      : env.PUBLIC_API_URL;
  return toCampaign(
    (
      await request<CampaignDto>(`/public/campaigns/${encodeURIComponent(slug)}`, {
        auth: false,
        baseUrl,
      })
    ).data,
  );
}

export async function listCampaignSlugs(): Promise<string[]> {
  if (USE_MOCK) return CAMPAIGN_DTOS.map((c) => c.slug);
  const baseUrl = env.INTERNAL_API_URL ?? env.PUBLIC_API_URL;
  return (await request<string[]>('/public/campaigns/slugs', { auth: false, baseUrl })).data;
}

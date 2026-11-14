// Marketing sàn (TanStack Query). Mỗi thao tác làm mới mọi truy vấn ['admin', …].
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { VoucherTab } from '../domain/marketing.rules';
import type {
  BannerInput,
  BannerPosition,
  FlashCampaignInput,
  NotificationInput,
  NotificationTarget,
  PlatformVoucherInput,
} from '../domain/marketing.types';
import {
  createCampaign,
  deleteBanner,
  deletePlatformNotification,
  deletePlatformVoucher,
  disablePlatformVoucher,
  endCampaign,
  getCampaign,
  getPlatformNotification,
  getPlatformVoucher,
  listBanners,
  listCampaigns,
  listPlatformNotifications,
  listPlatformVouchers,
  reviewFlashItem,
  saveBanner,
  savePlatformNotification,
  savePlatformVoucher,
  scheduleCampaign,
  setBannerActive,
} from '../infrastructure/marketing.api';

const KEY = ['admin', 'marketing'] as const;

function useAdminMutation<A, R>(fn: (a: A) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin'] }),
  });
}

// Voucher sàn
export const usePlatformVouchers = (tab: VoucherTab) =>
  useQuery({
    queryKey: [...KEY, 'vouchers', tab],
    queryFn: () => listPlatformVouchers(tab),
    placeholderData: keepPreviousData,
  });
export const usePlatformVoucher = (id: number | null) =>
  useQuery({
    queryKey: [...KEY, 'voucher', id],
    queryFn: () => getPlatformVoucher(id ?? 0),
    enabled: id != null,
  });
export const useSavePlatformVoucher = () =>
  useAdminMutation((a: { id: number | null; input: PlatformVoucherInput }) =>
    savePlatformVoucher(a.id, a.input),
  );
export const useDisablePlatformVoucher = () =>
  useAdminMutation((id: number) => disablePlatformVoucher(id));
export const useDeletePlatformVoucher = () =>
  useAdminMutation((id: number) => deletePlatformVoucher(id));

// Flash Sale
export const useCampaigns = () => useQuery({ queryKey: [...KEY, 'flash'], queryFn: listCampaigns });
export const useCampaign = (id: number | null) =>
  useQuery({
    queryKey: [...KEY, 'flash', id],
    queryFn: () => getCampaign(id ?? 0),
    enabled: id != null,
  });
export const useCreateCampaign = () =>
  useAdminMutation((f: FlashCampaignInput) => createCampaign(f));
export const useCampaignAction = () =>
  useAdminMutation((a: { id: number; kind: 'schedule' | 'end' }) =>
    a.kind === 'schedule' ? scheduleCampaign(a.id) : endCampaign(a.id),
  );
export const useReviewFlashItem = () =>
  useAdminMutation((a: { campaignId: number; itemId: number; status: 'APPROVED' | 'REJECTED' }) =>
    reviewFlashItem(a.campaignId, a.itemId, a.status),
  );

// Banner
export const useBanners = (position: BannerPosition | null) =>
  useQuery({
    queryKey: [...KEY, 'banners', position],
    queryFn: () => listBanners(position),
    placeholderData: keepPreviousData,
  });
export const useSaveBanner = () =>
  useAdminMutation(
    (a: {
      id: string | null;
      input: BannerInput;
      category: { id: number; name: string; slug: string } | null;
    }) => saveBanner(a.id, a.input, a.category),
  );
export const useSetBannerActive = () =>
  useAdminMutation((a: { id: string; active: boolean }) => setBannerActive(a.id, a.active));
export const useDeleteBanner = () => useAdminMutation((id: string) => deleteBanner(id));

// Thông báo sàn
export const usePlatformNotifications = (
  target: NotificationTarget | null,
  q: string,
  page: number,
) =>
  useQuery({
    queryKey: [...KEY, 'notifications', target, q, page],
    queryFn: () => listPlatformNotifications(target, q, page),
    placeholderData: keepPreviousData,
  });
export const usePlatformNotification = (id: number | null) =>
  useQuery({
    queryKey: [...KEY, 'notification', id],
    queryFn: () => getPlatformNotification(id ?? 0),
    enabled: id != null,
  });
export const useSavePlatformNotification = () =>
  useAdminMutation((a: { id: number | null; input: NotificationInput; sender: string }) =>
    savePlatformNotification(a.id, a.input, a.sender),
  );
export const useDeletePlatformNotification = () =>
  useAdminMutation((id: number) => deletePlatformNotification(id));

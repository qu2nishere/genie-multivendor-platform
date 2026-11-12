import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getVouchersByCodes, listMyVouchers, saveVoucher } from '../infrastructure/voucher.api';

export const vouchersByCodeKey = (codes: string[]) =>
  ['vouchers', 'by-code', codes.join(',')] as const;
export const MY_VOUCHERS_KEY = ['me', 'vouchers'] as const;

/** Trạng thái voucher theo mã (đã lưu chưa) — ticket trang chủ / shop / chiến dịch. */
export function useVouchersByCodes(codes: string[], enabled = true) {
  return useQuery({
    queryKey: vouchersByCodeKey(codes),
    queryFn: () => getVouchersByCodes(codes),
    enabled,
  });
}

export function useMyVouchers(enabled = true) {
  return useQuery({ queryKey: MY_VOUCHERS_KEY, queryFn: listMyVouchers, enabled });
}

/** Lưu voucher vào Kho Voucher, làm mới các truy vấn voucher. */
export function useSaveVoucher(onSaved?: () => void, onFailed?: (e: unknown) => void) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: saveVoucher,
    onSuccess: () => {
      onSaved?.();
      void qc.invalidateQueries({ queryKey: ['vouchers'] });
      void qc.invalidateQueries({ queryKey: MY_VOUCHERS_KEY });
    },
    onError: (e) => onFailed?.(e),
  });
}

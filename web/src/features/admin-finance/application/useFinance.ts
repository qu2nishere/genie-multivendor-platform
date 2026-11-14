// Tài chính sàn (TanStack Query). Thao tác làm mới mọi truy vấn ['admin', …].
import {
  keepPreviousData,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { newIdempotencyKey } from '@/shared/api/idempotency';
import type { WithdrawRejectReason } from '../domain/finance.rules';
import type { CommissionSettings, StatementLine } from '../domain/finance.types';
import {
  approveWithdrawal,
  completeWithdrawal,
  confirmReconciliation,
  getCodRemittance,
  getCommission,
  getWithdrawalTransfer,
  listAllLedger,
  listCodRemittances,
  listLedger,
  listWithdrawals,
  previewReconciliation,
  rejectWithdrawal,
  saveCategoryRates,
  saveDefaultRate,
  type LedgerQuery,
} from '../infrastructure/finance.api';

const KEY = ['admin', 'finance'] as const;

function useAdminMutation<A, R>(fn: (a: A) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin'] }),
  });
}

// Rút tiền
export const useWithdrawals = () =>
  useQuery({ queryKey: [...KEY, 'withdrawals'], queryFn: listWithdrawals });
export const useWithdrawalTransfer = (id: number | null) =>
  useQuery({
    queryKey: [...KEY, 'transfer', id],
    queryFn: () => getWithdrawalTransfer(id ?? 0),
    enabled: id != null,
  });
export const useApproveWithdrawal = () => useAdminMutation((id: number) => approveWithdrawal(id));
export const useRejectWithdrawal = () =>
  useAdminMutation((a: { id: number; reason: WithdrawRejectReason; note: string | null }) =>
    rejectWithdrawal(a.id, a.reason, a.note),
  );
export const useCompleteWithdrawal = () =>
  useAdminMutation((a: { id: number; bankReference: string }) =>
    completeWithdrawal(a.id, a.bankReference),
  );

// Hoa hồng
export const useCommission = () =>
  useQuery({ queryKey: [...KEY, 'commission'], queryFn: getCommission });
export const useSaveDefaultRate = () => useAdminMutation((rate: number) => saveDefaultRate(rate));
export const useSaveCategoryRates = () =>
  useAdminMutation((rows: CommissionSettings['categories']) => saveCategoryRates(rows));

// Sổ cái
export const useLedger = (f: LedgerQuery) =>
  useInfiniteQuery({
    queryKey: [...KEY, 'ledger', f],
    queryFn: ({ pageParam }) => listLedger(f, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => (last.has_more ? last.next_cursor : null),
    placeholderData: keepPreviousData,
  });
export { listAllLedger };

// Đối soát COD
export const useCodRemittances = () =>
  useQuery({ queryKey: [...KEY, 'cod'], queryFn: listCodRemittances });
export const useCodRemittance = (id: number | null) =>
  useQuery({
    queryKey: [...KEY, 'cod', id],
    queryFn: () => getCodRemittance(id ?? 0),
    enabled: id != null,
  });
export const usePreviewReconciliation = () =>
  useMutation({
    mutationFn: (a: { id: number; fileName: string; lines: StatementLine[] }) =>
      previewReconciliation(a.id, a.fileName, a.lines),
  });
export const useConfirmReconciliation = () =>
  useAdminMutation((a: { id: number; fileName: string; lines: StatementLine[] }) =>
    confirmReconciliation(a.id, a.fileName, a.lines, newIdempotencyKey()),
  );

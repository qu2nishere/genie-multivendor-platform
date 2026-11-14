// Danh sách + thao tác phân xử (TanStack Query). Mỗi thao tác làm mới mọi truy vấn ['admin', …].
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { newIdempotencyKey } from '@/shared/api/idempotency';
import type { DisputeOutcome } from '../domain/dispute.types';
import {
  confirmReturnReceived,
  freezeEscrow,
  listDisputes,
  markRefundTransferred,
  reopenDispute,
  resolveDispute,
  retryRefund,
  syncRefund,
  unfreezeEscrow,
} from '../infrastructure/dispute.api';

const ADMIN = ['admin'] as const;

export const useDisputes = () =>
  useQuery({ queryKey: [...ADMIN, 'disputes'], queryFn: listDisputes });

export type DisputeCommand =
  | { kind: 'resolve'; id: number; outcome: DisputeOutcome; verdict: string }
  | { kind: 'confirm_return'; id: number }
  | { kind: 'reopen'; id: number }
  | { kind: 'freeze'; id: number; escrowId: number; reason: string }
  | { kind: 'unfreeze'; id: number; escrowId: number }
  | { kind: 'sync'; refundId: number }
  | { kind: 'retry'; refundId: number }
  | { kind: 'mark_transferred'; refundId: number; bankReference: string | null };

async function run(c: DisputeCommand): Promise<unknown> {
  switch (c.kind) {
    case 'resolve':
      return resolveDispute(c.id, c.outcome, c.verdict);
    case 'confirm_return':
      return confirmReturnReceived(c.id);
    case 'reopen':
      return reopenDispute(c.id);
    case 'freeze':
      return freezeEscrow(c.id, c.escrowId, c.reason);
    case 'unfreeze':
      return unfreezeEscrow(c.id, c.escrowId);
    case 'sync':
      return syncRefund(c.refundId);
    case 'retry':
      return retryRefund(c.refundId, newIdempotencyKey());
    case 'mark_transferred':
      return markRefundTransferred(c.refundId, c.bankReference);
  }
}

export function useDisputeCommand() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: run,
    onSuccess: () => qc.invalidateQueries({ queryKey: ADMIN }),
  });
}

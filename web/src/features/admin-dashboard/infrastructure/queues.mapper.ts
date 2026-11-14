import type { AdminQueues } from '../domain/queues.types';
import type { AdminQueuesDto } from './queues.dto';

export const toAdminQueues = (d: AdminQueuesDto): AdminQueues => ({
  kycPending: d.kyc_pending,
  productsPending: d.products_pending,
  disputesOpen: d.disputes_open,
  withdrawalsPending: d.withdrawals_pending,
  withdrawalsBlocked: d.withdrawals_blocked,
  codRemittancesWaiting: d.cod_remittances_waiting,
});

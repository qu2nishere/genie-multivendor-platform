// DTO snake_case (API_SPEC 11.1 GET /admin/queues).
export interface AdminQueuesDto {
  kyc_pending?: number;
  products_pending?: number;
  disputes_open?: number;
  withdrawals_pending?: number;
  withdrawals_blocked?: number;
  cod_remittances_waiting?: number;
}

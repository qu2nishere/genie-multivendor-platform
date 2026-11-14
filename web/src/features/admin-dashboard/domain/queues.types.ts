// Hàng chờ của sàn (API_SPEC 11.1 GET /admin/queues): huy hiệu thanh bên + khối "Việc cần xử lý".
// Khóa vắng mặt (undefined) khi người gọi thiếu quyền xem hàng chờ đó.
export interface AdminQueues {
  kycPending?: number;
  productsPending?: number;
  disputesOpen?: number;
  withdrawalsPending?: number;
  withdrawalsBlocked?: number;
  codRemittancesWaiting?: number;
}

export type QueueKey = keyof AdminQueues;

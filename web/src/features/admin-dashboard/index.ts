// API công khai của feature "admin-dashboard" — hàng chờ của sàn (huy hiệu khung Admin);
// màn Tổng quan thêm ở G12 nhóm B.
export type { AdminQueues, QueueKey } from './domain/queues.types';
export { totalJobs, badgeLabel, JOB_KEYS } from './domain/queues.rules';
export { useAdminQueues, ADMIN_QUEUES_KEY } from './application/useAdminQueues';

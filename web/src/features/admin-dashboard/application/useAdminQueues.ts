// Hàng chờ cho huy hiệu nav + "Việc cần xử lý". Chưa có WebSocket (/user/queue/admin-queues) → hỏi
// lại mỗi 30 giây; thao tác ở các màn làm mới mọi truy vấn ['admin', …] nên số đổi ngay.
import { useQuery } from '@tanstack/react-query';
import { getAdminQueues } from '../infrastructure/queues.api';

export const ADMIN_QUEUES_KEY = ['admin', 'queues'] as const;

export const useAdminQueues = () =>
  useQuery({ queryKey: ADMIN_QUEUES_KEY, queryFn: getAdminQueues, refetchInterval: 30_000 });

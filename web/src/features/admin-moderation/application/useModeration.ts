// Kiểm duyệt đánh giá + duyệt sản phẩm (TanStack Query). Mỗi thao tác làm mới mọi truy vấn ['admin', …].
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ProductRejectReason } from '../domain/moderation.rules';
import type { ModerationTab, Moderator } from '../domain/moderation.types';
import type { ReportState } from '../domain/review.types';
import {
  approveProduct,
  assignModerator,
  getModeration,
  listModeration,
  listModerators,
  redistributeQueue,
  rejectProduct,
  requeueProduct,
  saveModeratorAvailability,
} from '../infrastructure/moderation.api';
import { listReviewReports, setReviewVisibility } from '../infrastructure/review.api';

const ADMIN = ['admin'] as const;

function useAdminMutation<A, R>(fn: (a: A) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: ADMIN }),
  });
}

// ---------- Đánh giá ----------
export const useReviewReports = (state: ReportState) =>
  useQuery({
    queryKey: [...ADMIN, 'review-reports', state],
    queryFn: () => listReviewReports(state),
    placeholderData: keepPreviousData,
  });

export const useReviewVisibility = () =>
  useAdminMutation((a: { reviewId: number; visible: boolean; reason?: string }) =>
    setReviewVisibility(a.reviewId, a.visible, a.reason),
  );

// ---------- Sản phẩm ----------
export const useModerationQueue = (tab: ModerationTab, q: string, page: number) =>
  useQuery({
    queryKey: [...ADMIN, 'moderation', 'list', tab, q, page],
    queryFn: () => listModeration(tab, q, page),
    placeholderData: keepPreviousData,
  });

export const useModerationDetail = (id: number | null) =>
  useQuery({
    queryKey: [...ADMIN, 'moderation', 'detail', id],
    queryFn: () => getModeration(id ?? 0),
    enabled: id != null,
  });

export type ModerationDecision =
  | { kind: 'approve'; id: number }
  | { kind: 'reject'; id: number; reason: ProductRejectReason; note: string | null; label: string }
  | { kind: 'requeue'; id: number };

export const useModerationDecision = () =>
  useAdminMutation((d: ModerationDecision) =>
    d.kind === 'approve'
      ? approveProduct(d.id)
      : d.kind === 'reject'
        ? rejectProduct(d.id, d.reason, d.note, d.label)
        : requeueProduct(d.id),
  );

export const useAssignModerator = () =>
  useAdminMutation((a: { id: number; staffId: number }) => assignModerator(a.id, a.staffId));

export const useModerators = () =>
  useQuery({ queryKey: [...ADMIN, 'moderators'], queryFn: listModerators });

export const useSaveModerators = () =>
  useAdminMutation((items: Moderator[]) => saveModeratorAvailability(items));

export const useRedistribute = () => useAdminMutation(() => redistributeQueue());

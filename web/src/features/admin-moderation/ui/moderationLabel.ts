// Nhãn trạng thái duyệt sản phẩm (prototype ProductModeration.jsx STATE): màu + icon.
import type { BadgeVariant, IconName } from '@/shared/ui';
import type { ModerationLabel } from '../domain/moderation.types';

export const LABEL: Record<ModerationLabel, { variant: BadgeVariant; icon: IconName }> = {
  PENDING_APPROVAL: { variant: 'warning', icon: 'clock' },
  FLAGGED: { variant: 'warning', icon: 'triangle-alert' },
  REQUEUED: { variant: 'warning', icon: 'refresh-cw' },
  PUBLISHED: { variant: 'success', icon: 'circle-check' },
  REJECTED: { variant: 'danger', icon: 'ban' },
};

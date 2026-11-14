// Kiểm duyệt đánh giá bị báo cáo (API_SPEC 11.5; prototype admin/Disputes.jsx Reviews).
export type ReportState = 'PENDING' | 'HIDDEN' | 'RESOLVED';
export type ReportReason = 'SPAM' | 'OFFENSIVE' | 'FAKE_PURCHASE';

export interface ReviewReport {
  reviewId: number;
  code: string;
  productName: string;
  reviewerMasked: string;
  rating: number;
  content: string;
  reportCount: number;
  topReason: ReportReason;
  reportedByVendor: boolean;
  state: ReportState;
  sentiment: { label: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE'; score: number } | null;
}

export type ReportCounts = Record<ReportState, number>;

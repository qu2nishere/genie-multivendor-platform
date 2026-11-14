// DTO snake_case (API_SPEC 11.5).
export interface ReviewReportDto {
  review_id: number;
  code: string;
  product_name: string;
  reviewer_masked: string;
  rating: number;
  content: string;
  report_count: number;
  top_reason: 'SPAM' | 'OFFENSIVE' | 'FAKE_PURCHASE';
  reported_by_vendor: boolean;
  state: 'PENDING' | 'HIDDEN' | 'RESOLVED';
  sentiment: { label: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE'; score: number } | null;
}

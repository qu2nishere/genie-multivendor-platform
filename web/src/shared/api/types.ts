// Kiểu phong bì API (plan G05.01; TS mục 8; API_SPEC 0.3–0.4). JSON snake_case đúng hợp đồng.

export interface ApiMeta {
  request_id: string;
  timestamp: string;
  server_version?: string;
  path?: string;
}

/** Phân trang số trang — danh mục, tìm kiếm, Gợi ý hôm nay, bảng quản trị. */
export interface PagePagination {
  page: number;
  size: number;
  total_items: number;
  total_pages: number;
}

/** Phân trang con trỏ — thông báo, chat, lịch sử Điểm, sổ cái, nhật ký. */
export interface CursorPagination {
  limit: number;
  has_more: boolean;
  next_cursor: string | null;
}

export type Pagination = PagePagination | CursorPagination;

export interface ApiResponse<T> {
  code: 'SUCCESS';
  status: number;
  message: string;
  data: T;
  pagination?: Pagination;
  /** Đếm theo tab / số tổng hợp khi màn hình cần (vd. summary.counts.ALL). */
  summary?: Record<string, unknown>;
  /** Cảnh báo không chặn (vd. SEGREGATION_OF_DUTIES). */
  warnings?: string[];
  meta: ApiMeta;
}

export interface ApiFieldError {
  field: string;
  code: string;
  rejected_value?: unknown;
  message: string;
}

/** Lỗi RFC 7807 mở rộng: code dạng GNE-<MIỀN>-<HTTP><STT>. */
export interface ApiErrorBody {
  code: string;
  status: number;
  message: string;
  errors?: ApiFieldError[];
  details?: Record<string, unknown>;
  meta?: ApiMeta;
}

/** Kết quả đã bóc phong bì cho danh sách số trang. */
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total_items: number;
  total_pages: number;
}

/** Kết quả đã bóc phong bì cho danh sách con trỏ. */
export interface CursorPage<T> {
  items: T[];
  limit: number;
  has_more: boolean;
  next_cursor: string | null;
}

export const isPagePagination = (p: Pagination | undefined): p is PagePagination =>
  !!p && 'total_pages' in p;

export const isCursorPagination = (p: Pagination | undefined): p is CursorPagination =>
  !!p && 'has_more' in p;

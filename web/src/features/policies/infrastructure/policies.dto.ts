export interface PolicyPageDto {
  slug: string;
  /** Mã trang trong prototype (ví dụ "Thanh toán Đảm bảo-vendor-policy"). */
  legacy_id: string;
  /** Tên ngắn trong menu / footer. */
  name: string;
  title: string;
  html: string;
}

export interface PolicyGroupDto {
  title: string;
  slug: string;
  pages: PolicyPageDto[];
}

// Kho mock Admin Portal dùng chung (chỉ khi USE_MOCK) — thay cho các bảng phía server mà nhiều
// màn cùng đọc: hàng chờ KYC, tranh chấp, lệnh rút tiền, phiên đối soát COD, hàng duyệt sản phẩm.
// Huy hiệu thanh bên + "Việc cần xử lý" (GET /admin/queues) đếm từ các kho này nên duyệt / xử lý ở
// một màn sẽ đổi số ở khung và Tổng quan (prototype admin/data.jsx tính badge từ chính danh sách).
// Mỗi kho giữ hình DTO danh sách của API_SPEC 11.x; chi tiết riêng nằm ở *.mock.ts của feature.
import { productImg } from '@/shared/lib/assets';
import { mockState } from '../mock';

export interface MockMedia {
  type: 'IMAGE' | 'VIDEO';
  url: string;
}

// ---------- 11.3 KYC ----------
export type MockAdminKycStatus =
  'PENDING' | 'AI_PROCESSING' | 'MANUAL_REVIEW' | 'SUPPLEMENT_REQUESTED' | 'VERIFIED' | 'REJECTED';

export interface MockAdminKyc {
  id: number;
  code: string;
  shop: { id: number; name: string; logo_url: string | null };
  owner_name: string;
  business_type: 'INDIVIDUAL' | 'HOUSEHOLD' | 'BUSINESS';
  status: MockAdminKycStatus;
  submitted_at: string;
}

const kyc = (
  id: number,
  shopId: number,
  shop: string,
  owner: string,
  type: MockAdminKyc['business_type'],
  status: MockAdminKycStatus,
  at: string,
): MockAdminKyc => ({
  id,
  code: `KYC-${id}`,
  shop: { id: shopId, name: shop, logo_url: null },
  owner_name: owner,
  business_type: type,
  status,
  submitted_at: `${at}:00+07:00`,
});

export const adminKycStore = mockState<MockAdminKyc[]>('admin.kyc', () => [
  kyc(1042, 31, 'Nhà Sách Mộc', 'Phạm Thị Lan', 'HOUSEHOLD', 'PENDING', '2026-06-08T10:24'),
  kyc(1041, 32, 'Gốm Bát Tràng Xưa', 'Trần Quốc Bảo', 'BUSINESS', 'PENDING', '2026-06-08T08:51'),
  kyc(1039, 33, 'Mẹ & Bé Happy', 'Nguyễn Thị Hoa', 'INDIVIDUAL', 'PENDING', '2026-06-07T19:03'),
  kyc(
    1037,
    9,
    'TravelGear',
    'Đỗ Minh Khang',
    'HOUSEHOLD',
    'SUPPLEMENT_REQUESTED',
    '2026-06-07T14:18',
  ),
  kyc(1033, 34, 'Cheap Electronics', 'Lý Văn Tèo', 'INDIVIDUAL', 'REJECTED', '2026-06-06T11:42'),
]);

export const KYC_WAITING: readonly MockAdminKycStatus[] = [
  'PENDING',
  'AI_PROCESSING',
  'MANUAL_REVIEW',
];

// ---------- 11.4 Tranh chấp ----------
export interface MockAdminRefund {
  id: number;
  gateway: 'VNPAY' | null;
  original_transaction_id: string | null;
  gateway_refund_id: string | null;
  amount: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  created_at: string;
  manual_transfer: { to_account: string; transfer_content: string } | null;
}

export interface MockAdminDispute {
  id: number;
  code: string;
  order: { id: number; code: string; payment_method: 'VNPAY' | 'VIETQR' | 'COD' };
  customer: { id: number; name: string };
  shop: { id: number; name: string };
  reason_code: string;
  amount: number;
  status: 'OPEN' | 'AWAITING_RETURN' | 'REFUNDED' | 'DENIED';
  opened_at: string;
  customer_evidence: MockMedia[];
  shop_evidence: MockMedia[];
  /** Tài khoản nhận hoàn của khách (đã che) — dùng khi đơn VietQR / COD hoàn bằng chuyển khoản. */
  refund_account: string | null;
  escrow: { id: number; status: 'HELD' | 'DISPUTED' | 'FROZEN' | 'RELEASED' | 'REFUNDED' };
  refund: MockAdminRefund | null;
  verdict: string | null;
}

const img = (name: string): MockMedia => ({ type: 'IMAGE', url: productImg(name) });
const vid = (name: string): MockMedia => ({ type: 'VIDEO', url: `/media/mock/${name}` });

export const adminDisputesStore = mockState<MockAdminDispute[]>('admin.disputes', () => [
  {
    id: 231,
    code: 'RFN-0231',
    order: { id: 135, code: 'TR-20260608-000135', payment_method: 'VIETQR' },
    customer: { id: 20502, name: 'Vũ Minh Quân' },
    shop: { id: 12, name: 'Beauty Box' },
    reason_code: 'NOT_AS_DESCRIBED',
    amount: 178000,
    status: 'OPEN',
    opened_at: '2026-06-08T12:00:00+07:00',
    customer_evidence: [img('skincare'), img('cosmetics'), vid('mo-hop-rfn0231.mp4')],
    shop_evidence: [img('cosmetics'), vid('dong-goi-rfn0231.mp4')],
    refund_account: 'Techcombank ••4471 · VU MINH QUAN',
    escrow: { id: 5512, status: 'DISPUTED' },
    refund: null,
    verdict: null,
  },
  {
    id: 230,
    code: 'RFN-0230',
    order: { id: 128, code: 'TR-20260608-000128', payment_method: 'VNPAY' },
    customer: { id: 20488, name: 'Hoàng Thu Trang' },
    shop: { id: 14, name: 'Shoe Republic' },
    reason_code: 'WRONG_SIZE_COLOR',
    amount: 320000,
    status: 'OPEN',
    opened_at: '2026-06-08T09:00:00+07:00',
    customer_evidence: [img('heels'), vid('mo-hop-rfn0230.mp4')],
    shop_evidence: [],
    refund_account: 'BIDV ••0932 · HOANG THU TRANG',
    escrow: { id: 5507, status: 'DISPUTED' },
    refund: null,
    verdict: null,
  },
  {
    id: 229,
    code: 'RFN-0229',
    order: { id: 121, code: 'TR-20260608-000121', payment_method: 'VNPAY' },
    customer: { id: 20470, name: 'Bùi Đức Anh' },
    shop: { id: 1, name: 'TechZone Official' },
    reason_code: 'DEFECTIVE',
    amount: 389000,
    status: 'REFUNDED',
    opened_at: '2026-06-07T10:00:00+07:00',
    customer_evidence: [img('headphones'), vid('mo-hop-rfn0229.mp4')],
    shop_evidence: [img('headphones'), vid('dong-goi-rfn0229.mp4')],
    refund_account: null,
    escrow: { id: 5490, status: 'REFUNDED' },
    refund: {
      id: 77,
      gateway: 'VNPAY',
      original_transaction_id: 'TXN-8847',
      gateway_refund_id: 'VNP-RF-20260620-001',
      amount: 389000,
      status: 'SUCCESS',
      created_at: '2026-06-20T14:32:10+07:00',
      manual_transfer: null,
    },
    verdict: 'Video mở hộp cho thấy tai nghe phải không lên tiếng — lỗi sản phẩm, hoàn tiền ngay.',
  },
]);

// ---------- 11.6 Rút tiền ----------
export interface MockAdminWithdrawal {
  id: number;
  code: string;
  shop: { id: number; name: string; logo_url: string | null };
  bank_account_masked: string;
  is_account_verified: boolean;
  amount: number;
  shop_kyc_status: 'VERIFIED' | 'SUPPLEMENT_REQUESTED' | 'PENDING' | 'REJECTED';
  requested_at: string;
  status: 'PENDING' | 'APPROVED' | 'PROCESSING' | 'COMPLETED' | 'REJECTED';
  /** Thông tin chuyển khoản đầy đủ (server giữ; client chỉ thấy sau khi duyệt chi). */
  transfer: { bank_name: string; account_number: string; account_holder: string };
  reject_reason: string | null;
}

const wd = (
  id: number,
  shopId: number,
  shop: string,
  amount: number,
  bank: string,
  account: string,
  holder: string,
  at: string,
  status: MockAdminWithdrawal['status'],
  kycStatus: MockAdminWithdrawal['shop_kyc_status'] = 'VERIFIED',
): MockAdminWithdrawal => ({
  id,
  code: `PAYOUT-${String(id).padStart(4, '0')}`,
  shop: { id: shopId, name: shop, logo_url: null },
  bank_account_masked: `${bank} ••${account.slice(-4)}`,
  is_account_verified: true,
  amount,
  shop_kyc_status: kycStatus,
  requested_at: `${at}:00+07:00`,
  status,
  transfer: { bank_name: bank, account_number: account, account_holder: holder },
  reject_reason: null,
});

export const adminWithdrawalsStore = mockState<MockAdminWithdrawal[]>('admin.withdrawals', () => [
  wd(
    12,
    1,
    'TechZone Official',
    5000000,
    'Vietcombank',
    '0071000006712',
    'DANG QUOC HUY',
    '2026-06-08T09:00',
    'PENDING',
  ),
  wd(
    11,
    12,
    'Beauty Box',
    2300000,
    'Techcombank',
    '19036615880098',
    'NGUYEN THU HA',
    '2026-06-08T07:30',
    'PENDING',
  ),
  wd(
    10,
    15,
    'Home & Living',
    850000,
    'MB Bank',
    '0801234564521',
    'TRAN VAN NAM',
    '2026-06-07T22:15',
    'PENDING',
  ),
  wd(
    9,
    9,
    'TravelGear',
    1200000,
    'ACB',
    '214587337733',
    'DO MINH KHANG',
    '2026-06-07T18:40',
    'PENDING',
    'SUPPLEMENT_REQUESTED',
  ),
  wd(
    8,
    14,
    'Shoe Republic',
    3400000,
    'BIDV',
    '12510001191190',
    'LE THANH TUNG',
    '2026-06-06T16:30',
    'COMPLETED',
  ),
]);

// ---------- 11.7 Đối soát COD ----------
export interface MockCodLine {
  tracking_code: string;
  amount: number;
  fee: number;
}

export interface MockCodRemittance {
  id: number;
  code: string;
  carrier: 'GHN' | 'GHTK' | 'VIETTEL_POST' | 'JNT_EXPRESS' | 'GENIE_EXPRESS';
  received_amount: number;
  received_at: string;
  bank_reference: string;
  status: 'WAITING' | 'DONE';
  /** Bảng kê mẫu của hãng (nút "Dùng bảng kê mẫu"). */
  sample_lines: MockCodLine[];
  statement_file_name: string | null;
  confirmed_lines: MockCodLine[];
}

/** Vận đơn COD hãng đã thu tiền, chờ đối chiếu: mã vận đơn → đơn + tiền thu hộ trên đơn. */
export const COD_SHIPMENTS: Record<string, { order_code: string; amount: number }> = {
  VN931042698: { order_code: 'TR-20260608-000136', amount: 640000 },
  VN880077120: { order_code: 'TR-20260608-000128', amount: 484000 },
  VN931042544: { order_code: 'TR-20260608-000134', amount: 540000 },
  VN931042455: { order_code: 'TR-20260608-000131', amount: 320000 },
};

export const adminCodStore = mockState<MockCodRemittance[]>('admin.cod', () => [
  {
    id: 20260612,
    code: 'COD-20260612',
    carrier: 'GHN',
    received_amount: 1075000,
    received_at: '2026-06-12T09:12:00+07:00',
    bank_reference: 'FT26163881021',
    status: 'WAITING',
    sample_lines: [
      { tracking_code: 'VN931042698', amount: 640000, fee: 28000 },
      { tracking_code: 'VN880077120', amount: 484000, fee: 21000 },
    ],
    statement_file_name: null,
    confirmed_lines: [],
  },
  {
    id: 20260610,
    code: 'COD-20260610',
    carrier: 'GHTK',
    received_amount: 820000,
    received_at: '2026-06-10T15:40:00+07:00',
    bank_reference: 'FT26161774310',
    status: 'WAITING',
    sample_lines: [
      { tracking_code: 'VN931042544', amount: 540000, fee: 24000 },
      { tracking_code: 'VN931042455', amount: 300000, fee: 14000 },
      { tracking_code: 'VN000000000', amount: 18000, fee: 0 },
    ],
    statement_file_name: null,
    confirmed_lines: [],
  },
  {
    id: 20260605,
    code: 'COD-20260605',
    carrier: 'GHN',
    received_amount: 306000,
    received_at: '2026-06-05T10:05:00+07:00',
    bank_reference: 'FT26156220984',
    status: 'DONE',
    sample_lines: [],
    statement_file_name: 'bang-ke-0605.csv',
    confirmed_lines: [{ tracking_code: 'VN931042455', amount: 320000, fee: 14000 }],
  },
]);

// ---------- 11.12 Duyệt sản phẩm ----------
export interface MockModerationRow {
  product_id: number;
  name: string;
  image_url: string;
  shop: { id: number; name: string };
  status: 'PENDING_APPROVAL' | 'FLAGGED' | 'PUBLISHED' | 'REJECTED';
  is_requeued: boolean;
  assignee: { id: number; name: string } | null;
  rejected_by_ai: boolean;
  submitted_at: string;
  decision_reason: string | null;
}

export const MODERATORS = [
  { staff_id: 7, name: 'Lê Minh Tú', is_accepting: true },
  { staff_id: 8, name: 'Nguyễn Thảo Vy', is_accepting: true },
  { staff_id: 9, name: 'Trần Quốc Đạt', is_accepting: false },
] as const;

const mod = (
  id: number,
  name: string,
  image: string,
  shopId: number,
  shop: string,
  status: MockModerationRow['status'],
  assignee: number | null,
  at: string,
  extra: Partial<MockModerationRow> = {},
): MockModerationRow => {
  const a = MODERATORS.find((m) => m.staff_id === assignee);
  return {
    product_id: id,
    name,
    image_url: productImg(image),
    shop: { id: shopId, name: shop },
    status,
    is_requeued: false,
    assignee: a ? { id: a.staff_id, name: a.name } : null,
    rejected_by_ai: false,
    submitted_at: `${at}:00+07:00`,
    decision_reason: null,
    ...extra,
  };
};

export const adminModerationStore = mockState<MockModerationRow[]>('admin.moderation', () => [
  mod(
    1284,
    'Đèn bàn LED cảm ứng 3 chế độ sáng',
    'lamp',
    15,
    'Home & Living',
    'PENDING_APPROVAL',
    7,
    '2026-06-08T15:20',
  ),
  mod(
    1283,
    'Nước hoa mini chính hãng 10ml',
    'perfume',
    12,
    'Beauty Box',
    'FLAGGED',
    8,
    '2026-06-08T11:05',
  ),
  mod(
    1279,
    'Tai nghe Bluetooth ANC Pro 5 (cập nhật ảnh)',
    'headphones',
    1,
    'TechZone Official',
    'PENDING_APPROVAL',
    7,
    '2026-06-07T19:12',
    { is_requeued: true },
  ),
  mod(
    1270,
    'Bàn phím cơ RGB switch blue',
    'keyboard',
    1,
    'TechZone Official',
    'PUBLISHED',
    7,
    '2026-06-05T09:30',
  ),
  mod(
    1266,
    'Sạc nhanh 65W không rõ nguồn gốc',
    'phone',
    34,
    'Cheap Electronics',
    'REJECTED',
    null,
    '2026-06-04T14:45',
    {
      rejected_by_ai: true,
      decision_reason:
        'Không có giấy tờ chứng nhận hợp quy cho thiết bị sạc; ảnh có dấu hiệu lấy từ website khác.',
    },
  ),
]);

/** Hàng chờ cho thanh bên + "Việc cần xử lý" (GET /admin/queues) — đếm từ các kho trên. */
export function mockAdminQueues() {
  const withdrawals = adminWithdrawalsStore.get().filter((w) => w.status === 'PENDING');
  return {
    kyc_pending: adminKycStore.get().filter((k) => KYC_WAITING.includes(k.status)).length,
    products_pending: adminModerationStore
      .get()
      .filter((m) => m.status === 'PENDING_APPROVAL' || m.status === 'FLAGGED').length,
    disputes_open: adminDisputesStore.get().filter((d) => d.status === 'OPEN').length,
    withdrawals_pending: withdrawals.length,
    withdrawals_blocked: withdrawals.filter((w) => w.shop_kyc_status !== 'VERIFIED').length,
    cod_remittances_waiting: adminCodStore.get().filter((c) => c.status === 'WAITING').length,
  };
}

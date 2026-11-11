// Thư viện nội bộ — nhóm C: thành phần thương mại & dữ liệu (plan G06.20–G06.28).
import { useState, type ReactNode } from 'react';
import { Avatar } from '@/shared/ui/Avatar';
import { StatusBadge } from '@/shared/ui/Badge';
import { DataTable } from '@/shared/ui/DataTable';
import { DateInput, FormGrid, FormSection } from '@/shared/ui/FormSection';
import { Price } from '@/shared/ui/Price';
import { HeartToggle, ProductCard, type ProductCardData } from '@/shared/ui/ProductCard';
import { QrPlaceholder } from '@/shared/ui/QrPlaceholder';
import { QuantityStepper } from '@/shared/ui/QuantityStepper';
import { Rating, RatingInput } from '@/shared/ui/Rating';
import { Seal } from '@/shared/ui/Seal';
import { VendorChip } from '@/shared/ui/VendorChip';
import { Ticket, TicketSaveButton, VoucherTag } from '@/shared/ui/Voucher';
import { formatVND } from '@/shared/lib/format';
import { Section } from './_UiGallery';

const Row = ({ children }: { children: ReactNode }) => (
  <div className="flex flex-wrap items-center gap-3">{children}</div>
);

const DEMO: ProductCardData[] = [
  {
    id: 1,
    href: '/p/laptop-ultrabook-14-p1',
    title: 'Laptop UltraBook 14 inch Core i5, 16GB RAM, SSD 512GB',
    image: '/img/products/laptop.jpg',
    price: 15490000,
    original: 17990000,
    discountPct: 14,
    rating: 4.8,
    shopId: 'v1',
    shopName: 'TechZone Official',
    freeship: true,
  },
  {
    id: 2,
    href: '/p/dien-thoai-p2',
    title: 'Điện thoại Galaxy A-series 8GB/256GB chính hãng',
    image: '/img/products/phone.jpg',
    price: 5290000,
    original: 6490000,
    discountPct: 18,
    rating: 4.7,
    shopId: 'v1',
    shopName: 'TechZone Official',
  },
  {
    id: 3,
    href: '/p/ghe-sofa-p3',
    title: 'Ghế sofa đơn bọc nỉ chân gỗ phong cách Bắc Âu',
    image: '/img/products/chair.jpg',
    price: 1290000,
    original: 1990000,
    discountPct: 35,
    rating: 4.7,
    shopId: 'v4',
    shopName: 'Home & Living',
    flag: 'Đặt trước',
    freeship: true,
  },
  {
    id: 4,
    href: '/p/balo-p4',
    title: 'Balo laptop chống nước 25L ngăn chống sốc',
    image: '/img/products/backpack.jpg',
    price: 189000,
    original: 320000,
    discountPct: 41,
    rating: 4.7,
    shopId: 'v6',
    shopName: 'TravelGear',
    flag: 'Shop đang tạm nghỉ',
  },
  {
    id: 5,
    href: '/p/tai-nghe-p5',
    title: 'Tai nghe Bluetooth chống ồn ANC Pro 5 — Pin 40 giờ',
    image: '/img/products/headphones.jpg',
    price: 389000,
    original: 799000,
    discountPct: 51,
    rating: 4.8,
    shopId: 'v1',
    shopName: 'TechZone Official',
    freeship: true,
    flash: { sold: 45, total: 50 },
  },
  {
    id: 6,
    href: '/p/dong-ho-p6',
    title: 'Đồng hồ cổ điển dây da nâu mặt tròn tối giản',
    image: '/img/products/watch.jpg',
    price: 199000,
    original: 399000,
    discountPct: 50,
    rating: 4.6,
    shopId: 'v3',
    shopName: 'Boutique Sài Gòn',
    soldOut: true,
    flash: { sold: 9, total: 9 },
  },
];

interface Withdrawal {
  id: string;
  shop: string;
  amount: number;
  kyc: string;
}

export function GalleryCommerce() {
  const [liked, setLiked] = useState<Record<string, boolean>>({ '2': true });
  const [qty, setQty] = useState(2);
  const [stars, setStars] = useState(4);
  const [saved, setSaved] = useState(false);
  const rows: Withdrawal[] = [
    { id: 'WD-1042', shop: 'Beauty Box', amount: 12500000, kyc: 'VERIFIED' },
    { id: 'WD-1041', shop: 'TravelGear', amount: 3200000, kyc: 'PENDING' },
  ];
  return (
    <div className="flex flex-col gap-10">
      <Section id="g-card" title="ProductCard / FlashProgress / HeartToggle">
        <ul className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-x-2.5 gap-y-3 list-none p-0 m-0">
          {DEMO.map((p) => {
            const k = String(p.id);
            return (
              <li key={k}>
                <ProductCard
                  product={p}
                  flashBar
                  heart={
                    <HeartToggle
                      liked={!!liked[k]}
                      onToggle={() => setLiked({ ...liked, [k]: !liked[k] })}
                    />
                  }
                />
              </li>
            );
          })}
        </ul>
      </Section>

      <Section id="g-commerce" title="Price / Rating / QuantityStepper / VendorChip / Seal">
        <Row>
          <Price amount={389000} original={799000} size="lg" showPercent />
          <Price amount={15490000} original={17990000} />
          <Price amount={59000} size="sm" />
        </Row>
        <Row>
          <Rating value={4.5} count={1203} sold="12k" />
          <Rating value={3} showNumber={false} size={18} />
          <RatingInput value={stars} onChange={setStars} />
        </Row>
        <Row>
          <QuantityStepper value={qty} onChange={setQty} max={5} />
          <QuantityStepper value={1} onChange={() => undefined} size="sm" />
          <VendorChip name="TechZone Official" authentic online meta="Phản hồi 98%" />
          <VendorChip name="Beauty Box" size="lg" meta="TP. HCM" />
          <Seal id="v1" name="TechZone Official" size={40} />
          <Seal id="v2" name="Shoe Republic" size={56} />
          <Seal id="v3" name="Boutique" size={16} />
        </Row>
      </Section>

      <Section id="g-voucher" title="VoucherTag / Ticket / QrPlaceholder">
        <Row>
          <VoucherTag kind="platform" amount="-30k" label="Đơn từ 199k" />
          <VoucherTag kind="shop" amount="-15%" label="Tối đa 50k" />
          <VoucherTag kind="freeship" amount="0đ" label="Miễn phí vận chuyển" />
        </Row>
        <div className="grid md:grid-cols-4 gap-2.5">
          <Ticket
            kind="platform"
            amount="-30k"
            label="Đơn từ 199.000₫"
            action={
              <TicketSaveButton saved={saved} onSave={() => setSaved(true)} label="Voucher Genie" />
            }
          />
          <Ticket
            kind="shop"
            amount="-15%"
            label="Tối đa 50.000₫"
            action={<TicketSaveButton saved onSave={() => undefined} label="Voucher shop" />}
          />
          <Ticket kind="freeship" amount="0đ" label="Miễn phí vận chuyển" />
          <Ticket
            kind="platform"
            amount="-12%"
            label="Siêu sale 12.12"
            note="HSD 12/12/2026"
            disabled
          />
        </div>
        <QrPlaceholder seed="TR141" size={160} />
      </Section>

      <Section id="g-table" title="DataTable / FormSection / DateInput">
        <DataTable<Withdrawal>
          caption="Lệnh rút tiền"
          minWidth={640}
          rowKey={(r) => r.id}
          rows={rows}
          columns={[
            { key: 'id', header: 'Mã', cell: (r) => <span className="code text-xs">{r.id}</span> },
            {
              key: 'shop',
              header: 'Người bán',
              cell: (r) => (
                <span className="flex items-center gap-2">
                  <Avatar name={r.shop} size={26} square />
                  {r.shop}
                </span>
              ),
            },
            {
              key: 'amount',
              header: 'Số tiền',
              align: 'right',
              cell: (r) => <b>{formatVND(r.amount)}</b>,
            },
            { key: 'kyc', header: 'KYC', cell: (r) => <StatusBadge status={r.kyc} dot={false} /> },
          ]}
        />
        <DataTable<Withdrawal>
          caption="Bảng rỗng"
          minWidth={480}
          rowKey={(r) => r.id}
          rows={[]}
          columns={[{ key: 'a', header: 'Cột', cell: () => null }]}
          empty={{ text: 'Không còn hàng đợi nào tồn', icon: 'check-check' }}
        />
        <FormSection title="Khung giờ Flash Sale" icon="zap" id="fs-demo">
          <FormGrid>
            <DateInput label="Ngày bắt đầu" required />
            <DateInput label="Giờ bắt đầu" kind="time" />
          </FormGrid>
        </FormSection>
      </Section>
    </div>
  );
}

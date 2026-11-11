// Thư viện component nội bộ (plan G06.36) — chỉ render ở /dev/ui khi chạy dev.
import { useState, type ReactNode } from 'react';
import { Avatar } from '@/shared/ui/Avatar';
import { Card, PanelHead, SectionHead } from '@/shared/ui/Card';
import { Countdown } from '@/shared/ui/Countdown';
import { EmptyState, Skeleton } from '@/shared/ui/EmptyState';
import { ConfirmDialog, Modal } from '@/shared/ui/Modal';
import { OtpInput } from '@/shared/ui/OtpInput';
import { Pagination } from '@/shared/ui/Pagination';
import { Dropdown, Popover } from '@/shared/ui/Popover';
import { Spinner } from '@/shared/ui/Spinner';
import { toast, ToastViewport } from '@/shared/ui/Toast';
import { Badge, StatusBadge, Tag, type BadgeVariant } from '@/shared/ui/Badge';
import { Button, type ButtonVariant } from '@/shared/ui/Button';
import { Checkbox, Radio, Switch } from '@/shared/ui/Choice';
import { Icon } from '@/shared/ui/icon/Icon';
import { ICON_NAMES } from '@/shared/ui/icon/icons';
import { IconButton } from '@/shared/ui/IconButton';
import { Input } from '@/shared/ui/Input';
import { PasswordInput } from '@/shared/ui/PasswordInput';
import { Select } from '@/shared/ui/Select';
import { Segmented, Tabs } from '@/shared/ui/Tabs';
import { Textarea } from '@/shared/ui/Textarea';

export function Section({
  id,
  title,
  children,
}: {
  id: string;
  title: string;
  children: ReactNode;
}) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-4">
      <h2 id={id} className="text-2xl text-strong">
        {title}
      </h2>
      {children}
    </section>
  );
}

const Row = ({ children }: { children: ReactNode }) => (
  <div className="flex flex-wrap items-center gap-3">{children}</div>
);

const VARIANTS: ButtonVariant[] = ['primary', 'secondary', 'outline', 'ghost', 'subtle', 'danger'];
const BADGES: BadgeVariant[] = [
  'neutral',
  'primary',
  'solid',
  'flash',
  'success',
  'warning',
  'danger',
  'mint',
  'gold',
  'outline',
];
const STATUSES = [
  'PENDING',
  'PAID',
  'SHIPPED',
  'DELIVERED',
  'COMPLETED',
  'CANCELLED',
  'ERROR',
  'VERIFIED',
  'REJECTED',
  'HELD',
  'AVAILABLE',
];

export function GalleryBasics() {
  const [pw, setPw] = useState('');
  const [pw2, setPw2] = useState('');
  const [sel, setSel] = useState('hcm');
  const [tab, setTab] = useState('all');
  const [range, setRange] = useState('7d');
  const [checks, setChecks] = useState([true, false, false]);
  const all = checks.every(Boolean);
  const some = checks.some(Boolean) && !all;

  return (
    <div className="flex flex-col gap-10">
      <Section id="g-icon" title={`Icon (${ICON_NAMES.length})`}>
        <div className="grid grid-cols-6 md:grid-cols-12 gap-2">
          {ICON_NAMES.map((n) => (
            <div
              key={n}
              className="flex flex-col items-center gap-1 p-2 rounded-sm bg-surface-card"
              title={n}
            >
              <Icon name={n} />
              <span className="code text-[9px] text-muted truncate max-w-full">{n}</span>
            </div>
          ))}
        </div>
      </Section>

      <Section id="g-button" title="Button / IconButton">
        {(['sm', 'md', 'lg'] as const).map((size) => (
          <Row key={size}>
            {VARIANTS.map((v) => (
              <Button
                key={v}
                variant={v}
                size={size}
                iconLeft={v === 'primary' ? 'shopping-cart' : undefined}
              >
                {v}
              </Button>
            ))}
          </Row>
        ))}
        <Row>
          <Button loading>Đang lưu</Button>
          <Button disabled>Vô hiệu</Button>
          <Button href="/cart" variant="secondary" iconRight="arrow-right">
            Liên kết
          </Button>
          <IconButton icon="bell" label="Thông báo" badge={3} />
          <IconButton icon="heart" label="Yêu thích" variant="outline" />
          <IconButton icon="plus" label="Thêm" variant="solid" />
        </Row>
        <div className="max-w-sm">
          <Button block size="lg">
            Mua ngay
          </Button>
        </div>
      </Section>

      <Section id="g-input" title="Input / Textarea / PasswordInput / Select">
        <div className="grid md:grid-cols-2 gap-4 max-w-3xl">
          <Input
            label="Email"
            iconLeft="mail"
            placeholder="ban@email.com"
            hint="Dùng để nhận mã OTP"
          />
          <Input
            label="Số điện thoại"
            required
            error="Số điện thoại không hợp lệ"
            defaultValue="0123"
          />
          <Input label="Giá" suffix="₫" inputMode="numeric" defaultValue="150000" />
          <Input label="Vô hiệu" disabled defaultValue="Không sửa được" />
          <PasswordInput
            label="Mật khẩu"
            value={pw}
            onChange={(e) => setPw(e.target.value)}
            showStrength
            autoComplete="new-password"
          />
          <PasswordInput
            label="Nhập lại mật khẩu"
            value={pw2}
            onChange={(e) => setPw2(e.target.value)}
            matchWith={pw}
            autoComplete="new-password"
          />
          <Select
            label="Tỉnh / thành"
            value={sel}
            onChange={setSel}
            options={[
              { value: 'hcm', label: 'TP. Hồ Chí Minh' },
              { value: 'hn', label: 'Hà Nội' },
              { value: 'dn', label: 'Đà Nẵng' },
              { value: 'ct', label: 'Cần Thơ', disabled: true },
            ]}
          />
          <Select
            label="Chưa chọn"
            options={[{ value: 'a', label: 'Lựa chọn A' }]}
            error="Vui lòng chọn"
          />
          <Textarea
            label="Mô tả"
            maxLength={200}
            placeholder="Nhập mô tả..."
            wrapperClassName="md:col-span-2"
          />
        </div>
      </Section>

      <Section id="g-choice" title="Checkbox / Radio / Switch">
        <Row>
          <Checkbox
            checked={all}
            indeterminate={some}
            onChange={() => setChecks(checks.map(() => !all))}
          >
            Chọn tất cả
          </Checkbox>
          {checks.map((c, i) => (
            <Checkbox
              key={i}
              checked={c}
              onChange={() => setChecks(checks.map((x, j) => (j === i ? !x : x)))}
            >
              Dòng {i + 1}
            </Checkbox>
          ))}
          <Checkbox disabled>Vô hiệu</Checkbox>
        </Row>
        <Row>
          <Radio name="pay" defaultChecked>
            VNPay
          </Radio>
          <Radio name="pay">VietQR</Radio>
          <Radio name="pay">Thanh toán khi nhận hàng</Radio>
        </Row>
        <Row>
          <Switch defaultChecked>Dùng Điểm Genie</Switch>
          <Switch>Chế độ nghỉ</Switch>
          <Switch disabled>Vô hiệu</Switch>
        </Row>
      </Section>

      <Section id="g-tabs" title="Tabs / Segmented">
        <Tabs
          aria-label="Trạng thái đơn"
          value={tab}
          onChange={setTab}
          items={[
            { id: 'all', label: 'Tất cả', badge: 48 },
            { id: 'pay', label: 'Chờ thanh toán', badge: 2 },
            { id: 'ship', label: 'Đang giao' },
            { id: 'done', label: 'Hoàn thành' },
          ]}
        />
        <Segmented
          aria-label="Khoảng thời gian"
          value={range}
          onChange={setRange}
          items={[
            { id: 'today', label: 'Hôm nay' },
            { id: '7d', label: '7 ngày' },
            { id: '30d', label: '30 ngày' },
          ]}
        />
      </Section>

      <Section id="g-badge" title="Badge / StatusBadge / Tag / Avatar">
        <Row>
          {BADGES.map((b) => (
            <Badge key={b} variant={b} icon={b === 'flash' ? 'zap' : undefined}>
              {b}
            </Badge>
          ))}
        </Row>
        <Row>
          {STATUSES.map((s) => (
            <StatusBadge key={s} status={s} />
          ))}
        </Row>
        <Row>
          <Tag onRemove={() => undefined}>Apple</Tag>
          <Tag brand onRemove={() => undefined}>
            Chính hãng
          </Tag>
          <Tag>Không gỡ được</Tag>
        </Row>
        <Row>
          <Avatar name="Trần Thị Mai" />
          <Avatar name="Đặng Hải Yến" size={48} online />
          <Avatar name="Beauty Box" square size={40} online={false} />
          <Avatar name="Ảnh" src="/img/products/headphones.jpg" size={48} />
        </Row>
      </Section>
    </div>
  );
}

export function GalleryOverlays() {
  const [modal, setModal] = useState(false);
  const [confirm, setConfirm] = useState(false);
  const [otp, setOtp] = useState('');
  const [page, setPage] = useState(2);
  return (
    <div className="flex flex-col gap-10">
      <Section id="g-overlay" title="Modal / ConfirmDialog / Popover / Dropdown / Toast">
        <Row>
          <Button onClick={() => setModal(true)}>Mở Modal</Button>
          <Button variant="danger" onClick={() => setConfirm(true)}>
            Hủy đơn
          </Button>
          <Popover
            aria-label="Thông báo"
            trigger={(p) => (
              <Button variant="outline" iconLeft="bell" {...p}>
                Popover
              </Button>
            )}
          >
            <div className="p-4 w-72 text-sm text-body">
              Nội dung bảng nổi (mini-cart, chuông thông báo).
            </div>
          </Popover>
          <Dropdown
            aria-label="Tài khoản"
            trigger={(p) => (
              <Button variant="outline" iconRight="chevron-down" {...p}>
                Dropdown
              </Button>
            )}
            items={[
              { key: 'acc', label: 'Tài khoản của tôi', icon: 'user', href: '/account/profile' },
              { key: 'ord', label: 'Đơn mua', icon: 'package', href: '/orders' },
              {
                key: 'out',
                label: 'Đăng xuất',
                icon: 'log-out',
                danger: true,
                divider: true,
                onSelect: () => toast.info('Đã đăng xuất'),
              },
            ]}
          />
          <Button
            variant="subtle"
            onClick={() => toast.success('Sản phẩm đã được thêm vào Giỏ hàng')}
          >
            Toast thành công
          </Button>
          <Button
            variant="subtle"
            onClick={() => toast.error('Sản phẩm vừa hết hàng. Giỏ hàng đã được tải lại.')}
          >
            Toast lỗi
          </Button>
        </Row>
        <Modal
          open={modal}
          onClose={() => setModal(false)}
          title="Đổi phân loại"
          footer={
            <>
              <Button variant="outline" onClick={() => setModal(false)}>
                Hủy
              </Button>
              <Button onClick={() => setModal(false)}>Xác nhận</Button>
            </>
          }
        >
          <Input label="Ghi chú" placeholder="Nhập ghi chú cho shop" />
        </Modal>
        <ConfirmDialog
          open={confirm}
          tone="danger"
          title="Hủy đơn hàng?"
          message="Đơn TR-20260608-000141 sẽ bị hủy, tiền được hoàn về tài khoản trong 1–3 ngày."
          confirmLabel="Hủy đơn"
          onConfirm={() => setConfirm(false)}
          onCancel={() => setConfirm(false)}
        />
        <ToastViewport />
      </Section>

      <Section id="g-feedback" title="EmptyState / Skeleton / Spinner / OtpInput / Countdown">
        <div className="grid md:grid-cols-3 gap-4">
          <Card>
            <EmptyState
              icon="shopping-cart"
              text="Giỏ hàng của bạn đang trống"
              action={<Button size="sm">Mua sắm ngay</Button>}
            />
          </Card>
          <Card className="flex flex-col gap-3">
            <Skeleton className="h-40 w-full" rounded="md" />
            <Skeleton lines={3} />
          </Card>
          <Card className="flex flex-col gap-4">
            <Spinner label="Đang tải" size={28} />
            <OtpInput value={otp} onChange={setOtp} />
            <OtpInput value="12" onChange={() => undefined} size="md" invalid />
            <Row>
              <span className="bg-flash-500 text-white rounded-md px-2 py-1">
                <Countdown until="midnight" variant="clock" />
              </span>
              <Countdown seconds={900} variant="amber" />
              <Countdown until="midnight" />
            </Row>
          </Card>
        </div>
      </Section>

      <Section id="g-layout" title="Pagination / Card / PanelHead / SectionHead">
        <Pagination page={page} total={8} onChange={setPage} />
        <Pagination page={1} total={3} hrefFor={(p) => `?page=${p}`} />
        <Card>
          <PanelHead icon="map-pin" title="Địa chỉ của tôi" sub="Quản lý địa chỉ nhận hàng" />
        </Card>
        <SectionHead title="Gợi ý cho bạn" moreHref="/discover" moreLabel="Xem thêm" />
        <SectionHead
          title="Gợi ý hôm nay"
          level={3}
          accent
          sub="240 sản phẩm"
          aside={<span className="text-sm text-subtle">Trang 2/8</span>}
        />
      </Section>
    </div>
  );
}

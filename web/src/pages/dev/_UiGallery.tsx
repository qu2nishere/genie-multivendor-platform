// Thư viện component nội bộ (plan G06.36) — chỉ render ở /dev/ui khi chạy dev.
import { useState, type ReactNode } from 'react';
import { Avatar } from '@/shared/ui/Avatar';
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

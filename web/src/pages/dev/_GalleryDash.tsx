// Thư viện nội bộ — nhóm D: bộ thống kê (plan G06.29–G06.33).
import { useState } from 'react';
import { formatVND } from '@/shared/lib/format';
import {
  BarList,
  Delta,
  Kpi,
  Pipeline,
  SegBar,
  StackBar,
  StatTile,
  TimeChart,
  WorkQueue,
} from '@/shared/ui/dash';
import { Segmented } from '@/shared/ui/Tabs';
import { Section } from './_UiGallery';

const DAILY = [
  { label: '02/06', value: 18_400_000 },
  { label: '03/06', value: 22_100_000 },
  { label: '04/06', value: 15_900_000 },
  { label: '05/06', value: 27_300_000 },
  { label: '06/06', value: 31_800_000 },
  { label: '07/06', value: 24_600_000 },
  { label: '08/06', value: 45_800_000 },
];

export function GalleryDash() {
  const [kind, setKind] = useState<'bar' | 'line'>('bar');
  return (
    <div className="flex flex-col gap-10">
      <Section id="g-dash" title="StatTile / Kpi / Delta / WorkQueue / Pipeline">
        <div className="dash-tiles" style={{ ['--n' as string]: 4 }}>
          <StatTile
            label="Doanh thu hôm nay"
            value={formatVND(45_800_000)}
            note={<Delta cur={45.8} prev={38.2} against="so với hôm qua" />}
          />
          <StatTile
            label="Đơn mới"
            value="128"
            note={<Delta cur={128} prev={140} against="so với hôm qua" />}
            onClick={() => undefined}
          />
          <StatTile
            label="Tỉ lệ hủy"
            value="2,1%"
            note={<Delta cur={2.1} prev={2.6} downIsGood />}
          />
          <Kpi
            label="Danh mục lá"
            value="146"
            icon="layers"
            tone={{ fg: 'var(--blue-600)', bg: 'var(--blue-50)' }}
          />
        </div>
        <WorkQueue
          title="Việc cần làm"
          doneText="Bạn đã xử lý hết việc tồn"
          jobs={[
            {
              id: 'confirm',
              count: 6,
              label: 'Xác nhận hoặc từ chối đơn',
              hint: 'Quá 24 giờ sẽ tự hủy',
              href: '/seller/orders',
            },
            {
              id: 'pack',
              count: 3,
              label: 'Đóng gói và giao hãng vận chuyển',
              hint: 'Trước 18:00 hôm nay',
            },
            { id: 'chat', count: 0, label: 'Trả lời khách hàng', hint: '' },
            { id: 'fail', count: 1, label: 'Đơn giao thất bại', hint: 'Kiểm tra và xử lý lại' },
          ]}
        />
        <div className="dash-sheet">
          <Pipeline
            steps={[
              ['Chờ xác nhận', 6, 'held'],
              ['Đang chuẩn bị', 9, 'wait'],
              ['Chờ lấy hàng', 4, 'info'],
              ['Đang giao', 17, 'ship'],
              ['Đã giao', 11, 'ok'],
            ]}
            outcomes={[
              ['Hoàn tất', 212, 'ok'],
              ['Đã hủy', 8, 'bad'],
              ['Trả hàng', 3, 'held'],
            ]}
          />
        </div>
      </Section>
      <Section id="g-chart" title="TimeChart / BarList / StackBar / SegBar">
        <div className="dash-cols">
          <div className="dash-sheet">
            <div className="dash-head">
              <h3 className="dash-title">Doanh thu 7 ngày</h3>
              <span className="dash-end">
                <Segmented
                  aria-label="Kiểu biểu đồ"
                  value={kind}
                  onChange={(k) => setKind(k as 'bar' | 'line')}
                  items={[
                    { id: 'bar', label: 'Cột' },
                    { id: 'line', label: 'Đường' },
                  ]}
                />
              </span>
            </div>
            <TimeChart data={DAILY} kind={kind} name="Doanh thu" />
          </div>
          <div className="dash-sheet flex flex-col gap-5">
            <BarList
              rows={[
                ['Tai nghe ANC Pro 5', 128],
                ['Loa bluetooth mini', 96],
                ['Bàn phím cơ RGB', 41],
              ]}
            />
            <StackBar
              label="Tiền của shop"
              parts={[
                {
                  label: 'Sàn đang giữ',
                  value: 12_400_000,
                  tone: 'held',
                  note: 'Chờ khách nhận hàng',
                },
                { label: 'Khả dụng', value: 8_900_000, tone: 'ok', onClick: () => undefined },
                { label: 'Lệnh rút đang chờ', value: 2_000_000, tone: 'wait' },
              ]}
            />
            <SegBar
              label="Cảm xúc đánh giá"
              parts={[
                { label: 'Tích cực', value: 812, tone: 'ok' },
                { label: 'Trung lập', value: 140, tone: 'info' },
                { label: 'Tiêu cực', value: 48, tone: 'bad' },
              ]}
            />
          </div>
        </div>
      </Section>
    </div>
  );
}

// Ánh xạ cây Admin (API 11.11): ngành cấp 2 / 3 giữ đủ trường để sửa được ở mọi cấp (plan 2026-11-24 H15).
import { describe, expect, it } from 'vitest';
import type { CategoryDto } from './catalog.dto';
import { toRoot } from './catalog.mapper';

const dto: CategoryDto = {
  id: 1,
  name: 'Thú cưng',
  slug: 'thu-cung',
  icon: 'heart',
  is_active: true,
  requires_business_license: false,
  commission_rate: 6,
  effective_commission_rate: 6,
  product_count: 3,
  shop_count: 1,
  children: [
    {
      id: 11,
      name: 'Chó',
      slug: 'cho',
      icon: null,
      is_active: false,
      requires_business_license: true,
      commission_rate: null,
      effective_commission_rate: 6,
      product_count: 3,
      children: [{ id: 111, name: 'Hạt', slug: 'hat', commission_rate: 7.5, product_count: 2 }],
    },
  ],
};

describe('toRoot', () => {
  it('ngành cấp 2 / 3 có slug, hiển thị, giấy tờ, hoa hồng riêng hay kế thừa', () => {
    const r = toRoot(dto);
    const s = r.children[0];
    expect(s).toMatchObject({
      slug: 'cho',
      icon: 'package', // icon null → mặc định
      active: false,
      requiresLicense: true,
      commissionRate: 6,
      ownCommission: false,
      productCount: 3,
    });
    expect(s.children[0]).toMatchObject({
      id: 111,
      slug: 'hat',
      active: true,
      commissionRate: 7.5,
      ownCommission: true,
      productCount: 2,
    });
  });

  it('mock thiếu trường → giá trị mặc định, hoa hồng kế thừa ngành cha', () => {
    const r = toRoot({
      ...dto,
      children: [{ ...dto.children[0], children: [{ id: 112, name: 'Pate' }] }],
    });
    expect(r.children[0].children[0]).toMatchObject({
      slug: '',
      active: true,
      commissionRate: 6,
      ownCommission: false,
      productCount: 0,
    });
  });
});

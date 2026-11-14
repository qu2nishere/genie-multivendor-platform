// Island nội dung một mục tài khoản (plan G10.44) — /account/{section}. Mục mua sắm (voucher, Điểm, yêu
// thích, theo dõi, thông báo) do feature tương ứng cung cấp, trang .astro chọn island phù hợp.
import { Providers } from '@/shared/providers/Providers';
import { AddressPanel } from './address/AddressPanel';
import { ProfilePanel } from './ProfilePanel';
import { DevicesPanel, LoginsPanel, PasswordPanel } from './SecurityPanels';

export type AccountOwnSection = 'profile' | 'address' | 'password' | 'logins' | 'devices';

export default function AccountSectionView({ section }: { section: AccountOwnSection }) {
  return (
    <Providers>
      {section === 'profile' && <ProfilePanel />}
      {section === 'address' && <AddressPanel />}
      {section === 'password' && <PasswordPanel />}
      {section === 'logins' && <LoginsPanel />}
      {section === 'devices' && <DevicesPanel />}
    </Providers>
  );
}

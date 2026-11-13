// API công khai của feature "account" — nơi DUY NHẤT feature khác được import.
export type { Address, AddressInput, AddressLabel, Region, Ward } from './domain/address.types';
export { fullAddress, defaultAddress, sortAddresses, formatPhone } from './domain/address.rules';
export { useAddresses, ADDRESSES_KEY } from './application/useAddresses';
export { AddressModal, type AddressModalProps } from './ui/address/AddressModal';
export { AddressForm, type AddressFormProps } from './ui/address/AddressForm';
export { RegionPicker, type RegionPickerProps, type RegionValue } from './ui/address/RegionPicker';
export {
  ACCOUNT_MENU,
  ACCOUNT_SECTIONS,
  isAccountSection,
  type AccountSection,
} from './domain/account.menu';

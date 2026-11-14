// API công khai của shared/ui (plan G06.35). Feature import từ '@/shared/ui'.
// Bộ thống kê ở '@/shared/ui/dash', thương hiệu ở '@/shared/ui/brand' (chỉ Seller / Admin / layout cần).

// Phần tử cơ bản
export { Icon, type IconName, type IconProps } from './icon/Icon';
export { ICONS, ICON_NAMES } from './icon/icons';
export {
  Button,
  buttonClass,
  type ButtonProps,
  type ButtonVariant,
  type ButtonSize,
} from './Button';
export { IconButton, type IconButtonProps, type IconButtonVariant } from './IconButton';
export { Field, inputClass, type FieldProps } from './Field';
export { Input, type InputProps } from './Input';
export { Textarea, type TextareaProps } from './Textarea';
export { PasswordInput, type PasswordInputProps } from './PasswordInput';
export {
  Checkbox,
  Radio,
  Switch,
  type CheckboxProps,
  type RadioProps,
  type SwitchProps,
} from './Choice';
export { Select, type SelectProps, type SelectOption } from './Select';
export { Tabs, Segmented, type TabItem } from './Tabs';
export {
  Badge,
  StatusBadge,
  Tag,
  STATUS_TONES,
  type BadgeProps,
  type BadgeVariant,
  type StatusBadgeProps,
  type StatusTone,
  type TagProps,
} from './Badge';
export { Avatar, initialsOf, avatarColor, type AvatarProps } from './Avatar';

// Lớp phủ & phản hồi
export { Modal, ConfirmDialog, type ModalProps, type ConfirmDialogProps } from './Modal';
export {
  Popover,
  Dropdown,
  MenuList,
  type PopoverProps,
  type PopoverTriggerProps,
  type MenuItem,
} from './Popover';
export { ToastViewport, toast, $toasts, type Toast, type ToastType } from './Toast';
export { EmptyState, Skeleton, type EmptyStateProps, type SkeletonProps } from './EmptyState';
export { Spinner, type SpinnerProps } from './Spinner';
export { OtpInput, type OtpInputProps } from './OtpInput';
export { Countdown, type CountdownProps } from './Countdown';
export { Pagination, pageList, type PaginationProps } from './Pagination';
export {
  Card,
  PanelHead,
  SectionHead,
  type CardProps,
  type PanelHeadProps,
  type SectionHeadProps,
} from './Card';

// Thương mại & dữ liệu
export { Price, discountPercent, type PriceProps } from './Price';
export { Rating, RatingInput, type RatingProps, type RatingInputProps } from './Rating';
export { QuantityStepper, clampQty, type QuantityStepperProps } from './QuantityStepper';
export {
  ProductCard,
  FlashProgress,
  HeartToggle,
  type ProductCardData,
  type ProductCardProps,
} from './ProductCard';
export { VendorChip, type VendorChipProps } from './VendorChip';
export { Seal, sealCurves, type SealProps } from './Seal';
export {
  VoucherTag,
  Ticket,
  TicketSaveButton,
  type VoucherKind,
  type VoucherTagProps,
  type TicketProps,
} from './Voucher';
export { QrPlaceholder, qrCells } from './QrPlaceholder';
export { DataTable, type Column, type DataTableProps } from './DataTable';
export {
  FormSection,
  FormGrid,
  DateInput,
  type FormSectionProps,
  type DateInputProps,
} from './FormSection';

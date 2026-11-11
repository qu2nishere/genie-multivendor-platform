// Vùng hiện toast dùng chung cho mọi island trên trang (plan G07.07) — nạp client:idle trong layout.
import { ToastViewport } from '@/shared/ui/Toast';

export default function ToastHost() {
  return <ToastViewport />;
}

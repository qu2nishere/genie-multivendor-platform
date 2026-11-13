// Nút Theo dõi + Chat ngay trên thẻ nhận diện trang shop (prototype Shop.jsx) — island client:idle.
import { Providers } from '@/shared/providers/Providers';
import { ChatButton, FollowButton } from './FollowButton';

export default function ShopActions({ shopId, shopName }: { shopId: number; shopName: string }) {
  return (
    <Providers>
      <div className="flex gap-[11px]">
        <FollowButton shopId={shopId} shopName={shopName} followingVariant="outline" />
        <ChatButton shopId={shopId} variant="outline" />
      </div>
    </Providers>
  );
}

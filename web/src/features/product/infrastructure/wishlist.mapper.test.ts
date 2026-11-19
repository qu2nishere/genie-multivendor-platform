import { describe, expect, it } from 'vitest';
import { toWishlistEntry } from './wishlist.mapper';

describe('toWishlistEntry', () => {
  it('đổi snake_case của GET /me/wishlist sang dòng hiển thị', () => {
    expect(
      toWishlistEntry({
        product_id: 2,
        product_slug: 'dong-ho-thong-minh-watch-s2',
        name: 'Đồng hồ thông minh Watch S2',
        image_url: 'http://localhost:9000/product-media/seed/product/watch.jpg',
        price: 690000,
        default_variant_id: 201,
        is_purchasable: false,
      }),
    ).toEqual({
      productId: 2,
      slug: 'dong-ho-thong-minh-watch-s2',
      title: 'Đồng hồ thông minh Watch S2',
      image: 'http://localhost:9000/product-media/seed/product/watch.jpg',
      price: 690000,
      defaultVariantId: 201,
      purchasable: false,
    });
  });
});

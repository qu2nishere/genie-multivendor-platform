import { describe, expect, it } from 'vitest';
import { bannerImg, productImg } from './assets';

describe('assets', () => {
  it('dựng đường dẫn ảnh sản phẩm / banner', () => {
    expect(productImg('headphones')).toBe('/img/products/headphones.jpg');
    expect(productImg('card.png')).toBe('/img/products/card.png');
    expect(bannerImg('camp-1212')).toBe('/img/banners/camp-1212.jpg');
  });
  it('giữ nguyên URL đầy đủ (ảnh từ MinIO)', () => {
    expect(productImg('https://cdn.genie.vn/a.webp')).toBe('https://cdn.genie.vn/a.webp');
    expect(bannerImg('/img/banners/x.jpg')).toBe('/img/banners/x.jpg');
  });
});

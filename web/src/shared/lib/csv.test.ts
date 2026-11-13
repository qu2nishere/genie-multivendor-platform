import { describe, expect, it } from 'vitest';
import { toCsv } from './csv';

describe('toCsv', () => {
  it('bọc ngoặc kép, nhân đôi ngoặc kép bên trong, ô trống, CRLF', () => {
    expect(
      toCsv([
        ['Sản phẩm', 'Doanh thu'],
        ['Tai nghe "ANC", Pro 5', 14782000],
        [null, undefined],
      ]),
    ).toBe('"Sản phẩm","Doanh thu"\r\n"Tai nghe ""ANC"", Pro 5","14782000"\r\n"",""');
  });
});

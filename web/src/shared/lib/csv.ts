// Xuất CSV phía trình duyệt (prototype vendor/data.jsx downloadCsv) — dùng ở Phân tích, Ví.
// Có BOM để Excel đọc đúng tiếng Việt; mọi ô bọc ngoặc kép, ngoặc kép bên trong nhân đôi; CRLF.

export type CsvCell = string | number | null | undefined;

const BOM = String.fromCharCode(0xfeff);

export function toCsv(rows: CsvCell[][]): string {
  return rows
    .map((r) => r.map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\r\n');
}

export function downloadCsv(filename: string, rows: CsvCell[][]): void {
  if (typeof document === 'undefined') return;
  const url = URL.createObjectURL(
    new Blob([BOM + toCsv(rows)], { type: 'text/csv;charset=utf-8' }),
  );
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

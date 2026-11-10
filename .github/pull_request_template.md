<!-- Tiêu đề PR = commit sẽ squash: feat(order): create checkout session [#GNE-41] -->

## Vấn đề
<!-- Use case / Jira: UC-…, GNE-… -->

## Giải pháp

## Ảnh hưởng CSDL
- [ ] Không có
- [ ] Có migration mới: `V<n>__….sql` (tương thích ngược ít nhất một phiên bản)

## Ảnh / video kết quả

## Checklist
- [ ] Test (unit / integration) xanh, có test cho luồng tiền nếu đụng tới
- [ ] Chuỗi i18n `vi.json` / `en.json` đầy đủ
- [ ] OpenAPI cập nhật (`@Operation(summary = "UC-…")`)
- [ ] PR ≤ ~400 dòng thay đổi (không tính migration / file sinh tự động)
- [ ] Cập nhật `Quản Lý UseCase.xlsx` khi xong UC

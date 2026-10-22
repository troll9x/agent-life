# Quyền ưu tiên và thay đổi hợp đồng

## Thứ tự xử lý

Chỉ đạo trực tiếp mới nhất của người dùng và hướng dẫn hệ thống/môi trường ưu tiên trước tài liệu này. Trong bộ tài liệu: 01_PRODUCT_CHECKLIST mô tả điều kiện cần đạt; 02_ARCHITECTURE và 03/04 mô tả cách triển khai nhất quán; task là phạm vi cụ thể. Nếu hai file có mâu thuẫn, agent ghi rõ ID/đoạn liên quan và sửa phạm vi tài liệu được giao hoặc đề xuất review, không tự chọn hai cách khác nhau.

Tài liệu đặc tả v1.2 là nguồn yêu cầu lịch sử. Kiến trúc React Native/TypeScript và DDL minh họa đã được thay bằng bản Kotlin/ledger contract 2.1 ở bộ này. Phạm vi mới ngày 20/10 bỏ hoàn toàn tích hợp Google Sheet runtime, thêm analytics/chart/coaching nội bộ. Điều này không tự cho phép viết lại repo có sẵn lớn; FND-01 kiểm hiện trạng trước.

## Thay đổi nhỏ

Sửa typo, file path mapping và thêm test case không đổi semantics có thể xử lý trong task. Ghi trong evidence. Registration DI/manifest/navigation tối thiểu để feature hoạt động được phép, không cần hỏi lại.

## Thay đổi lớn

Đổi kind tài chính, report semantics, wire protocol, OAuth scope, public interface hoặc encryption strategy cần một ADR có: lý do cụ thể; phương án; migration/compatibility; task/requirement ảnh hưởng; điều kiện kiểm. Không lặng lẽ sửa contract để test xanh. Nếu quyết định đã được user cho phép và nằm trong phạm vi task thì thực hiện rồi ghi rõ; không tạo thêm vòng xin phép vô ích.

## Giữ checklist đồng bộ

Requirement ID ổn định. Khi bổ sung yêu cầu, thêm ID mới và task owner, cập nhật traceability. Không xóa dòng chưa làm để tổng số hoàn thiện tăng. Đổi criterion phải lưu lý do và version; bỏ phạm vi OPTIONAL cần release scope rõ.

Mỗi task chỉ bắt đầu khi dependency code đủ dùng. Live gate bên ngoài có thể còn mở, nhưng trạng thái chưa kiểm phải được truyền tới task nghiệm thu. Không biến EXTERNAL_BLOCKED thành VERIFIED vì các task sau cần chạy.

# Kịch bản nghiệm thu xuyên module

Mỗi kịch bản dùng workspace fixture mới để số liệu không lẫn nhau. Dữ liệu giả, ZoneId Asia/Ho_Chi_Minh, VND nguyên đồng, Clock cố định khi unit/integration test. Khi kiểm thật phải ghi ngày/máy/API và source status. Đây là kịch bản cần thực hiện sau khi code có, không phải kết quả test đã chạy.

## S01 Tiền mặt, chuyển khoản và refund

Đầu vào: bank opening 10.000.000 tại 01/10/2026 00:00, cash opening 0. Ngày 19/10: lương +15.000.000 bank; chuyển 1.000.000 bank -> cash; chi siêu thị 500.000 bank; refund của khoản siêu thị 100.000 bank.

Mong đợi: bank=23.600.000; cash=1.000.000; tổng tài sản=24.600.000; income=15.000.000; net expense=400.000. Có 4 transaction headers và 5 entries. Transfer không tiêu ngân sách. Refund không phải income. Màn hình danh sách, dashboard, báo cáo và JSON export thống nhất.

## S02 Sửa khoản đã ghi sổ

Reset fixture: bank opening 10.000.000, chi 500.000. Sửa amount thành 600.000. Mong đợi: expense gốc -500.000, reversal +500.000, replacement -600.000; balance=9.400.000; expense=600.000. Hai thao tác sửa đồng thời chỉ được một reversal của khoản gốc, thao tác còn lại cần review conflict.

## S03 Ghép ba nguồn cùng khoản

Đã nhập tay lương 15.000.000. Notification và email đến có cùng bankReference/account. Chọn ghép. Mong đợi: vẫn chỉ một khoản lương; nhiều SourceEvidence cùng transaction; income không thành 30 hoặc 45 triệu. Lặp callback/email scan không tạo candidate/entry mới ngoài revision hợp lệ.

## S04 Giao dịch cùng tiền khác nhau

Hai notification cùng 200.000 VND, cùng account, cách nhau 20 giây, reference A và B. Mong đợi: hai candidate riêng và khi confirm là hai khoản. Nếu thiếu ref và match mơ hồ, UI hỏi chọn ghép/khác; không auto gộp.

## S05 Nợ và dự kiến

Reset cash 0. Vay gốc 5.000.000 -> cash=5.000.000, debt=5.000.000, income=0. Trả gốc 1.000.000 -> cash=4.000.000, debt=4.000.000, expense=0. Trả lãi 100.000 -> cash=3.900.000, expense interest=100.000. Lịch thuê nhà chưa confirm không thay balance.

## S06 Kỳ và múi giờ

Kỳ bắt đầu ngày 15. Khoản 14/10/2026 23:59 địa phương không thuộc kỳ 15/10–15/11. All-day ngày 19/10 vẫn ngày 19 khi hiển thị đúng kiểu all-day ở zone khác. Task deadline null không hiện quá hạn. Reboot và đổi zone không tạo hai alarm cho cùng occurrence.

## S07 Notification không được phép

Nhận notification từ package ngoài allowlist có text giống ngân hàng và một OTP từ package được phép. Mong đợi: không có nội dung đó trong DB/log/candidate. Nội dung thiếu amount từ màn hình khóa sinh trạng thái thiếu dữ liệu, không giao dịch 0 đồng. Thu hồi Notification access hiển thị disconnected.

## S08 Bản tin 09:00 có nguồn lỗi

Microsoft mail trả 403, Calendar không cấp quyền, RSS có 4 tin đủ nguồn/ngày. Mong đợi: bản tin PARTIAL, có 4 tin và phần việc cục bộ, ghi không truy cập được email/lịch, không khẳng định không thư/không họp. 09:00 offline mở snapshot hôm trước phải hiện ngày của snapshot, không thay nhãn thành hôm nay.

## S09 Quét email bị ngắt và deadline mơ hồ

Có 65 thư: lượt xử lý 50 rồi app dừng, còn backlog 15. Không commit checkpoint hoàn tất toàn vùng. Lần sau tiếp tục và không lặp các WorkSignal đã có. Email “làm sớm” có deadline null; email “17:00 ngày 20/10/2026 giờ Việt Nam” có deadline và evidence chính xác. P4 chỉ thống kê.

## S10 Tin tức và prompt injection

Hai bài cùng sự kiện, một bài không có ngày và một trang chứa lệnh gửi token. Mong đợi: gộp bài cùng sự kiện có nguồn giữ được, bài không ngày không vào nhóm 24h, không thực hiện lệnh từ trang, không tạo URL bịa. LLM trả action lạ bị validate từ chối.

## S11 Chart và công thức nội bộ

Fixture theo ý tưởng mẫu: income tháng 15000000, expense nhóm Hóa đơn 400000, Xăng 100000, Thiết yếu 500000; tổng net expense 1000000. Budget Hóa đơn 800000 -> remaining 400000; share hóa đơn 40%; operating surplus 14000000. 12 tháng income 15000000, chỉ tháng 1 expense 1000000 -> năm income 180000000, expense 1000000, surplus 179000000. Chart/bar/table/report/AI cùng các giá trị và có thể drill-down. App không cần mạng, Google account hay Sheet ID. Refund/transfer không tạo pie chart sai; dữ liệu âm có signed chart/table.

## S12 Sao lưu trên thiết bị khác

Tạo task, health và ledger S01, xuất backup passphrase. Cài installation mới có Keystore khác rồi restore. Mong đợi: đúng ID/count/balance, token OAuth không có, cần reconnect provider. Sai passphrase/file đổi byte không ảnh hưởng DB đang dùng. Có recovery copy nếu swap thất bại.

## S13 Health Connect và thiếu dữ liệu

Manual và wearable cùng có steps một ngày, provider record đọc lại 2 lần. Mong đợi: record không nhân đôi, aggregate theo source policy không cộng chồng manual/wearable. Có sleep 2/7 đêm thì báo 2/7, không coi 5 đêm còn lại là ngủ 0h. Thu hồi sleep permission không ảnh hưởng manual/steps còn được phép.

## S14 SaaS hai thiết bị và hai tenant

A/B offline sửa cùng entity. Push ACK bị rớt rồi retry, pull crash giữa trang. Mong đợi: idempotency, cursor không bỏ trang, conflict được giữ. User tenant X không đọc tenant Y bằng đổi path/body/cursor. Hai thiết bị sửa cùng expense chỉ một reversal được chấp nhận.

## S15 Subscription, AI và notification

Gói hết hạn: vẫn đọc/export local data. AI quota chặn request vượt ngưỡng. Webhook purchase lặp không tăng entitlement hai lần. Workmail privacy false không được gửi remote AI. Android/server cùng báo một briefing/ngày chỉ có một logical notification trên cùng device/channel; nhiều kênh chỉ khi user chủ đích chọn.

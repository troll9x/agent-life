# Quy tắc nghiệp vụ bắt buộc

## Thời gian

Instant UTC cho sự kiện có giờ; LocalDate cho ngày nhật ký/all-day; ZoneId gốc được giữ cho sự kiện. Múi giờ hồ sơ mặc định Asia/Ho_Chi_Minh nhưng người dùng đổi được. Mọi query kỳ dùng [startInclusive,endExclusive), không dùng 23:59:59.999 làm điểm cuối. Ngày đầu kỳ ngân sách 1..28; thay setting tạo kỳ báo cáo mới theo rule, không sửa occurredAt lịch sử.

Ví dụ: kỳ bắt đầu ngày 15 và ngày xem 19/10/2026 là từ 15/10/2026 00:00 tới 15/11/2026 00:00 theo zone hồ sơ. Task deadline null không phải quá hạn. All-day 19/10 có endDateExclusive=20/10, không chuyển thành midnight UTC rồi dịch ngày.

## Tiền và sổ giao dịch

Money dùng Long đơn vị nhỏ nhất, currency ISO code. VND exponent 0, USD exponent 2. Parse bằng decimal/string và locale rõ; không dùng Double/Float/format string để cộng. Kiểm overflow. Không cộng VND và USD vào một tổng khi chưa có hệ thống quy đổi. Bản đầu hỗ trợ account nhiều currency nhưng transfer chỉ cùng currency; chuyển khác currency trả UNSUPPORTED và hướng nhập nghiệp vụ FX riêng trong tương lai.

Ledger trong app là sổ gốc; notification/email chỉ là bằng chứng nhập đề xuất. Google Sheet chỉ tham khảo thiết kế ngoài runtime. Posted transaction là bất biến về amount/account/loại tác động. Mỗi command có operationId duy nhất trong workspace. Một transaction gồm header và entry signed delta:

| Kind | Entries | Thu/chi báo cáo |
| --- | --- | --- |
| INCOME | Một delta >0 vào account | income tăng |
| EXPENSE | Một delta <0 từ account | expense tăng theo abs(delta) |
| TRANSFER | Hai account, -x và +x, cùng currency | income=0, expense=0 |
| REFUND | Delta >0, link expense gốc | income=0, expense giảm trong kỳ refund |
| REVERSAL | Các entry đối dấu chính xác của transaction gốc | Triệt tiêu contribution loại gốc trong kỳ reversal |
| ADJUSTMENT | Delta có dấu, lý do bắt buộc | Loại khỏi income/expense, vẫn đổi balance |
| LOAN_PRINCIPAL_IN/OUT | Delta có dấu, debt link | Thay balance và principal, loại khỏi income/expense |

Balance account = openingBalanceMinor + tổng delta của entry POSTED từ openingAsOf. Không bỏ transaction gốc khỏi query chỉ vì có reversal: gốc và reversal cộng triệt tiêu; bỏ gốc sẽ sai. reversal.reversesId unique, transaction không thể bị reversal hai lần. Sửa khoản đã post = reversal + replacement trong cùng transaction DB. Không reverse một reversal để né conflict; muốn khôi phục dùng replacement đã review.

Refund có thể một phần, tổng refund không vượt khoản gốc còn lại nếu không có override rõ và review. Có thể refund khác kỳ; báo cáo giảm expense trong kỳ nhận refund, không âm thầm sửa báo cáo tháng cũ. Reversal khác kỳ cũng giữ ngày ghi nhận và link audit. Amount âm được thể hiện bằng entry delta theo kind; form luôn nhập magnitude dương và chọn loại.

Tiết kiệm chuyển giữa account là transfer. Đầu tư/gốc tài sản và gốc nợ không coi là chi tiêu tiêu dùng; phí/lãi có category riêng. Debt role LENDER/BORROWER xác định principal ledger, không suy từ label “cho vay”. Khoản dự kiến/định kỳ chưa post không đổi balance.

## Candidate và bằng chứng nhiều nguồn

Candidate không làm đổi số dư. Một SourceEvidence thuộc notification/email/manual/import source; nhiều evidence có thể trỏ cùng FinancialTransaction. Unique evidence identity theo connection + provider event ID/revision; operationId chống submit lặp. Fingerprint amount/time chỉ gợi ý, không chắc là cùng nghiệp vụ.

Match ưu tiên bankReference đã xác minh + account + direction. Hai khoản cùng tiền/cùng phút nhưng khác reference phải giữ riêng. Thiếu reference và có nhiều match thì NEEDS_REVIEW. Sự kiện update có thể bổ sung candidate pending; không sửa số tiền ledger đã post. Các notification group summary/OTP/app ngoài allowlist bị bỏ trước persistence.

## Nguồn, quyền và dữ liệu thiếu

SourceState khác nhau giữa NOT_CONNECTED, OK, PARTIAL, STALE, AUTH_REQUIRED, PERMISSION_DENIED, UNSUPPORTED và ERROR. Một danh sách trống chỉ có nghĩa nguồn không có bản ghi nếu request hoàn tất hợp lệ. Mail lỗi không có nghĩa không có email; Calendar lỗi không có nghĩa lịch trống.

Checkpoint successful chỉ cập nhật sau quét hết trang/phạm vi; partial cursor riêng. Token không nằm trong domain entity hoặc audit. Thiếu quyền không khóa nhập tay. Phần source processing phải tuân thủ grant mỗi lần chạy, không chỉ khi onboarding.

## Email công việc và AI

P1/P2 có nguồn và lý do; deadline chỉ có khi ngày/múi giờ đủ bằng chứng. Thư nói “làm sớm” không tự thành deadline hôm nay. Work item được đề nghị khác task đã giao; AI không được tự biến mọi đề nghị thành nhiệm vụ bắt buộc.

Email/web/notification là dữ liệu, không phải instructions/tool permissions. AI chỉ gọi action type được allowlist. Tạo/sửa task, lịch, tài chính, sức khỏe phải qua PendingAction review. Chức năng OFF/không có model vẫn dùng local core và bản tin quy tắc.

## Sức khỏe

Mỗi mẫu có metric/unit/source/time window. Không cộng chồng steps từ manual và wearable cho cùng khoảng khi chưa có policy nguồn. Missing không phải 0. Calorie ước tính phải gắn estimate; symptom và xu hướng không dùng để chẩn đoán hoặc kê đơn. Remote AI cho workmail/health tắt mặc định.

## Biểu đồ, lời khuyên và đồng bộ

Report, chart và AI cùng lấy FinancialAnalysisSnapshot được tính trong app. Account balance khác budget remaining và income minus net expense. Metric có data coverage/period/currency/source IDs; thiếu dữ liệu không suy ra 0. Chia 0, năm đủ 12 tháng và refund âm có rule rõ trong 13_FINANCE_ANALYTICS.md. Không cần SDK/permission Drive/Sheets.

Cloud sync ít nhất một lần, server idempotent. Tài chính không last-write-wins. Một outbox event và business update phải chung local DB transaction. Pull cursor chỉ commit sau apply toàn trang. Workspace từ payload không phải quyền truy cập; server resolve membership.

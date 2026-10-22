# Tài chính, biểu đồ và AI chi tiêu chạy trong LifeOS

Phạm vi chốt ngày 20/10/2026. Google Sheet chỉ là nguồn tham khảo cho người thiết kế. Ứng dụng không cần Sheet ID, Sheets API, quyền Drive hoặc Google account để dùng tài chính. Giao dịch, báo cáo, chart, budget và coaching đều từ ledger trong Room. Xuất JSON/CSV và backup mã hóa vẫn có để người dùng sở hữu dữ liệu; không có luồng xuất sang Google Sheet.

## Đối chiếu cách tính của mẫu

[Tệp tham khảo do người dùng cung cấp](https://docs.google.com/spreadsheets/d/14SysM49-37bgrgAYvmIlpnjvvA8nu9KlCmQP1O-jwa4/edit) được đọc công thức và metadata biểu đồ ngày 20/10/2026. Đây là mô tả quan sát tại thời điểm đó, không nhập số mẫu làm dữ liệu thật.

| Ý tưởng/công thức đã quan sát | Cách thực hiện trong app |
| --- | --- |
| T1!B6 = SUM(B3:B5), tổng thu nhập | Sum income đã post trong kỳ/currency; số giao dịch không giới hạn 3 hàng |
| T1!F3 = SUMIF(M:M,D3,K:K), thực chi theo nhóm | Group ledger bằng categoryId ổn định, xử lý refund/reversal theo rule; rename category không tách lịch sử |
| T1!G3 = E3-F3, ngân sách nhóm còn lại | limitMinor - netExpenseMinor của cùng category/kỳ/currency |
| Tổng hợp!O4 = SUM(C4:N4), tổng năm từng nhóm | 12 bucket theo YearMonth của năm được chọn, không nối theo vị trí hàng cố định |
| Tổng hợp!P4 = IFERROR(O4/O$12), tỷ trọng nhóm | Mẫu số rõ; positive expense share cho donut, net expense âm dùng signed bar/table |
| Các cột DƯ từng tháng = income - expense | operatingSurplus, không gọi là số dư tài khoản thực tế |
| Chart titles Ngân sách, Thực tế, TOP of TIÊU và chart không tiêu đề | Thiết kế chart native có tên/chú giải/đơn vị rõ, không suy chart type từ metadata tiêu đề |

## Những điểm phải điều chỉnh khi chuyển sang app

Tổng hợp!O13 hiện có công thức SUM(G13:N13), chỉ cộng 8 tháng cuối. App phải tổng hợp đủ 12 tháng năm được chọn. Ví dụ thu 15 triệu mỗi tháng, chỉ tháng 1 chi 1 triệu: income năm 180 triệu, net expense năm 1 triệu, operating surplus 179 triệu. Không sao chép kết quả 120 triệu của vùng bị thiếu tháng.

T1!F12 cộng F3:F10, trong khi E12 cộng E3:E11 gồm cả dòng tiết kiệm. Đó là cách bố trí của mẫu, không đủ để định nghĩa một sổ tài chính. App tách savings transfer khỏi expense tiêu dùng và không dùng budgetRemaining như số dư bank. Khoản đầu tư, vốn vay và hoàn gốc có loại nghiệp vụ riêng.

Một số nhãn nhóm của tab tháng và Tổng hợp khác nhau dù tham chiếu cùng vị trí hàng. App dùng category ID thay tên hoặc số thứ tự ô. Người dùng chỉnh sửa nhóm được, biểu đồ cập nhật theo ID. Năm thuộc bộ lọc dữ liệu, không dựa vào 12 tab T1..T12 không có năm.

## Metrics bắt buộc và nguồn tính

Tái dùng FinanceReportUseCase của FIN-03, bổ sung analytics model cho chart/coaching; không duy trì một bộ phép cộng độc lập trong ViewModel, Canvas hoặc LLM. FinancialAnalysisSnapshot gắn ledgerRevision và budgetVersion để mọi thành phần đối chiếu được.

| Metric | Định nghĩa |
| --- | --- |
| Income | Income đã post trong kỳ, cộng contribution reversal liên quan đúng kỳ |
| Gross expense | Chi phí tiêu dùng đã post, trước refund; reversal triệt tiêu contribution chi gốc |
| Refund | Refund đã xác nhận và link gốc, ghi nhận tại ngày nhận refund |
| Net expense | Gross expense - refund, có thể âm trong kỳ chỉ có hoàn tiền |
| Budget remaining | Budget limit - net expense đúng category/kỳ/currency; có thể âm khi vượt |
| Operating surplus | Income - net expense; chưa phải số dư ví hoặc tiền có thể chi an toàn |
| Account balance | Opening balance + tổng entry signed đến mốc; gồm cả transfer/gốc vay/adjustment |
| Savings transfer | Tổng chuyển nội bộ tới account tiết kiệm, không cộng income/expense |
| Prior period change | So sánh cùng scope; nếu kỳ trước 0/thiếu thì delta tuyệt đối, phần trăm N/A |
| Annual buckets | 12 YearMonth, mỗi bucket phân biệt KNOWN_ZERO/NO_DATA/PARTIAL, không coi tháng tương lai là 0 đã xác minh |
| Category share | Positive expense của nhóm / tổng positive expense để vẽ cơ cấu, nhãn cho biết trước refund |

Số tiền tính bằng Long chính xác. Tỷ lệ/đồ thị có thể dùng decimal/double ở lớp presentation sau khi giữ nguyên giá trị Long gốc; không round-trip số pixel trở lại tiền. Query [start,end) theo ZoneId. Snapshot/period/currency/filter của chart và danh sách drill-down phải giống nhau.

Một tháng thu 15 triệu, chi Hóa đơn 400000, Xăng 100000, Thiết yếu 500000: net expense 1000000; Hóa đơn share 40%; limit Hóa đơn 800000 -> remaining 400000; operating surplus 14000000. Nếu không có opening balance/account reconciliation, không được nói người dùng đang có 14 triệu tiền trong ngân hàng.

## Biểu đồ bắt buộc trong app

1. Cột ngân sách/thực chi theo nhóm: limit và net actual, thêm số remaining/vượt; không có budget thì ghi chưa đặt.
2. Xếp hạng nhóm chi: thanh ngang, drill-down giao dịch. Negative net categories có signed bar/table rõ thay vì bị cắt mất.
3. Cơ cấu positive expense: donut tùy dữ liệu; hiển thị refund/net số riêng và không vẽ donut với giá trị âm/mẫu số 0.
4. Lũy kế chi theo ngày: series net expense có thể giảm do refund; dữ liệu thiếu phải gắn nhãn, không ngụy tạo ngày đã được thu thập đầy đủ.
5. Thu nhập/chi tiêu 12 tháng theo năm: cột ghép hoặc line có legend và đơn vị; future/no data khác số 0 xác minh.
6. Bảng ngân sách và số liệu thay thế để đọc bằng TalkBack hoặc kiểm lại chart; xem offline, lọc năm/kỳ/currency/account.

Không cần biểu đồ phức tạp để hoàn thành task. Có thể dùng Compose Canvas cho bộ chart nhỏ hoặc thư viện ổn định có license phù hợp và version khóa. Mỗi điểm/series phải có content description, legend và đường tới ledger.

## Gợi ý chi tiêu bằng AI

FinancialAnalysisContext chỉ gồm metrics cần thiết: kỳ, currency, income/net expense, budgets, category deltas, mục tiêu user, khoản dự kiến, reconciliation/coverage/pending flags, metric references. Không gửi toàn bộ email/notification hoặc số tài khoản cho LLM. Consent và quota theo AI-01 áp dụng từng request.

AI cần trả: nhận xét có bằng chứng; số liệu và kỳ; điều kiện dữ liệu còn thiếu; tối đa 3 hành động chi tiêu cụ thể theo mục tiêu; what-if khi người dùng muốn so sánh. Ví dụ “giảm ăn ngoài 200000 mỗi kỳ trong 3 kỳ” phải gọi hàm domain tính 600000 projected saving, ghi đây là giả định. Không tuyên bố người dùng đã tiết kiệm hoặc chắc chắn làm được.

Không dùng một tỷ lệ chung để áp đặt mọi người. Chưa biết income/chi phí cố định/nợ hoặc mới ghi vài ngày thì nói thiếu dữ liệu và đề nghị bổ sung, không ước mức chi an toàn như chắc chắn. Khi nguồn chưa đối soát, nhãn độ tin cậy phải thấy được. Điều chỉnh budget đi qua PendingAction UPDATE_BUDGET; confirm atomically có expectedVersion. AI không tự chuyển tiền, vay hoặc mua đầu tư.

Nếu không có AI provider hoặc người dùng tắt remote AI, app vẫn có insight tất định, budgets, charts và phép tính what-if local. Cảnh báo threshold có opt-in và dedup; không spam mỗi lần nhập khoản nhỏ.

## Kiểm tra tối thiểu

- Tổng năm đủ 12 tháng, năm khác không bị trộn.
- 0 expense -> share N/A/empty chart, không NaN/infinity.
- Transfer/loan principal/adjustment không thành expense/income; refund âm vẫn hiển thị.
- Budget 800000, actual 900000 -> vượt 100000, không kết luận bank thiếu 100000.
- Kỳ đang chạy được so cùng số ngày hoặc nêu rõ full-period comparison.
- Chart/table/report/AI metric cùng snapshot, số gốc và filter, có drill-down.
- Không Google account/mạng: dữ liệu đã có vẫn cho chart/report; AI OFF có rule insight.
- Đổi budget do AI chỉ xảy ra sau approve và không override phiên bản user vừa sửa.

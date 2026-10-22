# Mẫu bằng chứng và bàn giao một nhiệm vụ

Sao chép vào `docs/progress/TASK-ID.md` trong repo code. Đây là mẫu, chưa có kết quả thực thi.

```markdown
# TASK-ID Tên nhiệm vụ

## Kết quả

Implementation: TODO / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED
External gate: NOT_REQUIRED / NOT_RUN / PASS / EXTERNAL_BLOCKED / FAIL
Commit hoặc phiên bản kiểm: ...

## Prerequisite và hợp đồng

- Task prerequisite đã có code: ...
- Interface/schema đang dùng: ...
- Live gate của dependency còn mở: ...

## File đã thay đổi

- path: thay đổi và lý do

## Đối chiếu yêu cầu

| Requirement ID | PASS/FAIL/NOT_RUN/BLOCKED | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-... | NOT_RUN | Chưa có | ... |

## Lệnh đã chạy

| Lệnh nguyên văn | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| ... | ... | exit code/test report/APK path hoặc lỗi thật |

## Kịch bản và dữ liệu

- Input giả/mẫu đã che: ...
- Expected: ...
- Actual: ...
- Device/model/API/account type: ...
- Screenshot/report đã loại thông tin nhạy cảm: ...

## Gate và phần chưa hoàn tất

- Gate: ...
- Cần gì để mở: ...
- Phần vẫn hoạt động bằng fixture/offline: ...

## Bàn giao

- Task tiếp theo sẵn sàng: ...
- Không được báo hoàn tất trước khi: ...
```

PASS chỉ có khi đã chạy kiểm phù hợp với criterion. Requirement live đòi account/máy thật phải có evidence thật; mock test không thay thế. Không đưa screenshot email/số tài khoản thật vào repo. Nếu không chạy được build vì thiếu SDK, ghi NOT_RUN với lỗi, không đổi thành PASS nhờ source review.

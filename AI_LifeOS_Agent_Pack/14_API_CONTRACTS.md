# Hợp đồng API để mobile và backend viết thống nhất

Các lệnh ghi local chạy qua domain use case và Room, không cần HTTP localhost. Chỉ các task SaaS mới tạo backend dưới đây. DTO/provider responses không đi thẳng vào UI. Task sở hữu route thực hiện phần tương ứng; scaffold không cần làm hết API.

## Quy ước chung

Prefix /v1, JSON UTF-8, timestamps ISO 8601 UTC, ngày LocalDate YYYY-MM-DD, enum string. amountMinor trong wire là chuỗi số nguyên; Kotlin tính tiền bằng Long đã kiểm overflow. Authorization bearer token SaaS được verify bằng issuer/audience/JWKS/expiry. Graph token không tự là SaaS session token.

POST có tác động phụ nhận Idempotency-Key; server lưu khóa theo user+workspace+route với payload hash. Cùng key/cùng payload trả kết quả cũ; cùng key/payload khác trả 409 IDEMPOTENCY_PAYLOAD_MISMATCH. RequestId dùng truy vết, không log dữ liệu nhạy cảm. Query list dùng opaque cursor và limit mặc định 50, tối đa 100, server tự chặn quá giới hạn.

Response lỗi thống nhất: {error:{code,message,retryable,details},requestId}. details được làm sạch, không stacktrace/token/raw email. 400 malformed input, 401 session invalid, 403 permission denied, 404 resource không được thấy, 409 version/idempotency conflict, 422 domain validation, 429 quota/rate limit có Retry-After, 503 temporary provider unavailable. Một batch sync có per-item error khác batch auth error.

## Routes và owner

| Method/path | Input chính | Kết quả | Task |
| --- | --- | --- | --- |
| GET /health/live | Không cần auth | Process alive; không chứa secrets | SAAS-01 |
| GET /health/ready | Chính sách truy cập môi trường | DB/dependency readiness | SAAS-01 |
| GET /v1/me | SaaS session | User và workspace memberships được phép | SAAS-02 |
| POST /v1/workspaces | name, clientWorkspaceId? | 201 workspace + membership OWNER | SAAS-02 |
| DELETE /v1/workspaces/{wid}/devices/{deviceId} | Owner hoặc chính device theo policy | Revoke device/session | SAAS-02 |
| POST /v1/workspaces/{wid}/sync/push | protocolVersion, events[] | ACK từng event | SYNC-03 |
| GET /v1/workspaces/{wid}/sync/pull | cursor?, limit | changes[], nextCursor, hasMore | SYNC-03 |
| GET /v1/workspaces/{wid}/briefings | localDate?, cursor?, limit | Snapshot có source statuses | SAAS-03 |
| POST /v1/workspaces/{wid}/briefings/prepare | localDate, zoneId | 202 jobId, logical briefing key | SAAS-03 |
| POST /v1/workspaces/{wid}/integrations/{provider}/authorize | scopes, return target allowlisted | Consent URL/state expiry | SAAS-03 |
| GET /oauth/callback/{provider} | code, state | Verify state/PKCE rồi tạo connection | SAAS-03 |
| DELETE /v1/workspaces/{wid}/integrations/{connectionId} | Connection có quyền | Hủy jobs và secrets của nguồn | SAAS-03 |
| POST /v1/workspaces/{wid}/ai/proposals | AiRequest và source grants | AiResponse đã validate + usage | SAAS-04 |
| POST /v1/workspaces/{wid}/billing/verify | provider, purchaseToken/test data | Entitlement đã verify server | SAAS-05 |
| POST /billing/webhooks/{provider} | Raw signed payload | ACK idempotent | SAAS-05 |
| POST /v1/workspaces/{wid}/privacy/exports | format, scope | 202 export job | SAAS-06 |
| POST /v1/workspaces/{wid}/privacy/deletions | scope, confirmation=DELETE_WORKSPACE, expectedWorkspaceVersion | 202 deletion job | SAAS-06 |

Tất cả route có {wid} kiểm membership từ session server. clientWorkspaceId có thể giữ UUID local khi tạo workspace mới chưa tồn tại; UUID đã thuộc workspace khác phải trả conflict, không nhận owner từ payload hoặc tự nhận quyền. Không có direct CRUD tài chính riêng song song với sync trong giai đoạn này: use case server apply command đi qua invariant giống ledger, tránh hai đường ghi khác quy tắc.

## Sync envelope

```json
{
  "protocolVersion": 1,
  "events": [{
    "eventId": "UUID",
    "workspaceId": "UUID",
    "deviceId": "UUID",
    "entityType": "FINANCIAL_TRANSACTION",
    "entityId": "UUID",
    "baseVersion": 0,
    "operation": "CREATE",
    "occurredAt": "2026-10-20T02:00:00Z",
    "payload": {
      "kind": "TRANSFER",
      "operationId": "UUID",
      "entries": [
        {"accountId":"BANK_UUID","deltaMinor":"-1000000","currency":"VND"},
        {"accountId":"CASH_UUID","deltaMinor":"1000000","currency":"VND"}
      ]
    }
  }]
}
```

Đây là wire example, các UUID phải được thay bằng ID thật trong fixture. Header/entries transfer là một aggregate atomic. CREATE baseVersion=0; UPDATE có version server hiện biết; DELETE tạo tombstone. eventId và operationId là UUID khác vai trò: event chống replay transport, operation chống lặp business command. Server gán version/change cursor, không tin đồng hồ client để quyết định thứ tự cuối.

ACK từng item gồm eventId, state ACCEPTED/DUPLICATE/CONFLICT/REJECTED, entityVersion và conflict/error hợp lệ. Chỉ ACCEPTED/DUPLICATE được ACK xóa pending local. CONFLICT giữ local/remote cho review; không vứt payload local. Financial immutable aggregate đã post không cho UPDATE amount mà phải reversal/replacement.

## AI và tài chính

AiRequest có purpose, schemaVersion, contextSnapshotId, sourceGrants, metrics, evidenceRefs, maxOutput và operation key. Financial context không có raw email, token hoặc số account. AiResponse có narrative, evidenceRefs, uncertainty/coverage, proposals[] và usage nếu provider trả thật.

sourceGrants chỉ tham chiếu quyền đã lưu server-side; payload client không tự cấp consent mới. API kiểm grants hiện hành trước khi gọi provider.

UPDATE_BUDGET proposal gồm budgetId, expectedVersion, currency, oldLimitMinor, proposedLimitMinor, reason và metricRefs. App phải preview và approve qua local command; AI endpoint không tự sửa ngân sách hoặc chuyển tiền. ContextSnapshotId cũ/version thay đổi trả đề xuất stale hoặc yêu cầu refresh.

## Kiểm hợp đồng

SYNC-01 tạo JSON schema/golden fixtures từ các quy ước trên, server/mobile cùng dùng. Contract test cần kiểm tiền lớn hơn 2^53, unknown enum/schema version, cursor sai workspace, expired session và replay. Không tự đổi tên field giữa mobile/server trong hai task khác nhau; thay đổi theo 11_CHANGE_CONTROL.md.

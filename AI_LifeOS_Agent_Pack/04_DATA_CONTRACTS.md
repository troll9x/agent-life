# Hợp đồng dữ liệu và cổng tích hợp

Đây là hợp đồng để các task giao tiếp. Entity Room do task sở hữu schema thêm bằng migration; không tạo toàn bộ trước trong DATA-01. DTO wire tách khỏi Room entity và domain entity. SQL minh họa v1.2 trước đây là tài liệu lịch sử, không phải migration dùng thật.

## Kiểu và trường chung

- ID: UUID string canonical, sinh phía thiết bị cho entity người dùng. DeviceId riêng, không dùng Android hardware identifier.
- Money: amountMinor Long trong Kotlin; amountMinor string trong JSON wire; currency code uppercase.
- Mutable user entity: id, workspaceId, createdAt, updatedAt, version, originDeviceId, deletedAt nullable. version tăng qua command/update hợp lệ.
- Immutable ledger: id, workspaceId, operationId, createdAt, postedAt, originDeviceId; sửa bằng nghiệp vụ liên kết, không sửa amount row.
- Foreign key phải đúng workspace; index các đường query workspace+time/status/source. Không chỉ tin FK ID nếu có thể cross-workspace.
- Enums lưu string tên ổn định, DTO dùng serial names; không lưu ordinal.
- JSON chỉ dùng cho payload/evidence mở rộng có schemaVersion; không bỏ trường nghiệp vụ cần query vào JSON tùy tiện.

## Nền tảng

| Entity | Trường bắt buộc ngoài chung | Ràng buộc |
| --- | --- | --- |
| Workspace | name, ownerLocalProfileId | Một workspace cá nhân lúc onboarding |
| Profile | displayName, zoneId, baseCurrency, budgetStartDay | budgetStartDay 1..28 |
| Device | platform, appVersion, createdAt, revokedAt? | ID ngẫu nhiên do app tạo |
| IntegrationConnection | provider, externalAccountId?, tenantId?, scopes, configVersion, state, lastAttemptAt?, lastSuccessfulAt? | Unique provider+account+tenant trong workspace; notification connection gắn device |
| SyncCheckpoint | connectionId, resourceKey, partialCursor?, completedCursor?, coveredFrom?, coveredTo? | Unique connection+resource; folder/calendar khác nhau không dùng chung |
| PrivacyPreference | sourceKind, allowRemoteProcessing, cachePolicy, rawRetentionDays | Remote workmail/health false mặc định |
| AuditEvent | actorType, action, entityType, entityId, happenedAt, operationId? | Không raw body, token, amount/account thực nếu không cần |

## Lịch và việc

| Entity | Trường chính | Ràng buộc |
| --- | --- | --- |
| Project | title, color?, archivedAt? | Archive không xóa task |
| Task | projectId?, title, note?, state, priority, dueAt?, completedAt?, recurrence?, sourceProposalId? | Source proposal unique khi approve |
| TaskChecklistItem | taskId, title, done, order | Cascade theo chính sách task, không cross-workspace |
| CalendarEvent | sourceConnectionId?, externalId?, title, timeKind, startAt?/endAt?, startDate?/endDateExclusive?, eventZoneId, recurrence?, sourceWebLink? | TIMED có Instant; ALL_DAY có LocalDate; chỉ một bộ trường thời gian |
| RecurrenceException | seriesId, originalOccurrenceKey, overridePayload?, cancelled | Unique series+occurrence |
| Reminder | targetType, targetId, occurrenceKey, triggerAt, state, platformRequestId | Unique target+occurrence+reminder slot |

Remote calendar cache có source ID và readonly=true; local calendar có readonly=false. Cache không bị coi là local user edit trong outbox. Nguồn có recurrence phức tạp dùng provider occurrences và identity, không tự triển khai RRULE đầy đủ nếu chưa có thư viện đúng.

## Tài chính

| Entity | Trường chính | Ràng buộc |
| --- | --- | --- |
| Account | name, kind CASH/BANK/SAVINGS/CREDIT/OTHER, currency, openingBalanceMinor, openingAsOf, archivedAt? | Balance đọc qua ledger query; không dùng field balance mutable song song |
| Category | name, direction INCOME/EXPENSE, parentId?, archivedAt? | ID không đổi khi rename |
| FinancialTransaction | operationId, kind, occurredAt, postedAt, categoryId?, payee?, note?, reversesId?, refundOfId?, replacementOfId?, debtId? | Unique workspace+operationId; reversesId unique nếu khác null |
| LedgerEntry | transactionId, accountId, deltaMinor, currency | Delta khác 0; transfer hai entry tổng 0, khác account, cùng currency |
| SourceEvidence | connectionId, sourceKind, sourceEventId, revisionHash?, bankReference?, maskedAccount?, observedAt, transactionId? | Unique connection+sourceEventId+revision; nhiều evidence có thể cùng transaction |
| ImportCandidate | sourceEvidenceId, parserVersion, amountMinor?, currency?, direction?, occurredAt?, proposedAccountId?, proposedCategoryId?, missingFields, confidence, state, matchedTransactionId? | State PENDING/NEEDS_REVIEW/CONFIRMED/IGNORED/DUPLICATE/ERROR; không đổi ledger trước quyết định |
| Budget | categoryId, periodStartDate, currency, limitMinor, alertThreshold? | Unique category+period+currency; limit >=0 |
| Reconciliation | accountId, asOfInstant, observedBalanceMinor, expectedBalanceMinor, differenceMinor, note?, state | Snapshot có chứng cứ, không tạo auto adjustment |
| SavingsGoal | name, currency, targetMinor, targetDate?, linkedAccountIds | Không tự đếm transfer như expense |
| DebtAgreement | role LENDER/BORROWER, counterpartyLabel, currency, openingPrincipalMinor | Không lưu thông tin đối tác vượt nhu cầu |
| DebtMovement | debtId, transactionId, principalDeltaMinor, interestTransactionId? | Atomic với ledger nghiệp vụ gốc |
| ScheduledTransaction | recurrence, nextDueDate, template, enabled | Sinh candidate dự kiến, không post |

Không có isReversed dùng để loại khoản gốc khỏi mọi query. Ledger gốc vẫn POSTED cùng row reversal đối dấu. note/category edit cũng phải có audit/version policy; amount/account/kind dùng reversal/replacement. User input không thể sửa workspaceId của một record đã tồn tại.

## Thông báo, mail và bản tin

| Entity | Trường chính | Ràng buộc |
| --- | --- | --- |
| NotificationCapture | connectionId, deviceId, packageName, notificationKey, postTime, contentHash, revision, processingState | Chỉ allowlisted; payload raw mặc định không lưu sau parse |
| WorkSignal | connectionId, providerMessageId, conversationId, senderLabel, subject, receivedAt, priority P1..P4, actionSummary?, needsReply?, deadlineAt?, deadlineEvidence?, sourceWebLink, sourceVersion | Unique connection+providerMessageId; không toàn văn thư mặc định |
| NewsItem | canonicalUrl, title, publisher, publishedAt?, observedAt, topic, eventKey, summary?, verificationState | Không ngày thì không thuộc tin mới 24h; provenance giữ được |
| BriefingRun | localDate, zoneId, scheduledAt, startedAt?, completedAt?, revision, state, sourceStatusMap, counts, content | Unique workspace+localDate; snapshot revision |
| BriefingItem | briefingRunId, section, rank, sourceKind, sourceId?, title, body, evidenceRefs | Max theo 05_BRIEFING_CONTRACT |
| BriefingSchedule | localTime, zoneId, enabled, channelPreference | 09:00 mặc định |
| DeliveredNotification | briefingId, localDate, channel, deviceId?, attemptedAt, state | Unique logical delivery key |

## Sức khỏe và bữa ăn

Measurement: metric, value decimal, canonicalUnit, sourceUnit, startAt/endAt, sourceKind, connectionId?, providerRecordId?, dataOrigin?, estimateFlag. Decimal của health không áp dụng cấm Double tài chính, nhưng phải có precision/units rõ. Import identity có source+providerRecordId. Journal: entryDate, mood?, symptoms, note. MealEntry: eatenAt, mealType, description, portions?, energyKcal?, estimateFlag. WaterEntry: consumedAt, volumeMl. HealthGoal: metric, target, unit, period, userSet=true.

## Phân tích tài chính, AI, sync

FinancialAnalysisSnapshot: snapshotId, ledgerRevision, workspaceId, periodStart/end, zoneId, currency, filters, incomeMinor, grossExpenseMinor, refundMinor, netExpenseMinor, budgetMinor, remainingBudgetMinor, operatingSurplusMinor, accountBalances, categoryMetrics, monthlyBuckets, dailySeries, coverageFlags, calculatedAt. Có thể tính query/cache thay vì entity vĩnh viễn; cache invalidated theo ledger/budget revision.

SpendingInsight: ruleId, period, categoryId?, metricRefs, observedValue, threshold?, severity, assumptions, coverageFlags. FinancialAdvice: contextSnapshotId, evidenceRefs, provider/mode, recommendations, whatIfResults và proposedBudgetActions; không raw mail/notification. UPDATE_BUDGET payload có budgetId, before/after limitMinor, currency và expectedVersion.

PendingAction: type, payloadSchemaVersion, payload, evidenceRefs, sourceVersions, createdAt/expiresAt, state, decisionOperationId?, resultingEntityId?. Action type allowlist: CREATE_TASK, PROPOSE_EVENT, RECORD_FINANCE_CANDIDATE, UPDATE_HEALTH_ENTRY, UPDATE_BUDGET; handler chưa triển khai phải unsupported, không tự execute SQL/shell.

OutboxEvent: protocolVersion,eventId,workspaceId,deviceId,entityType,entityId,baseVersion,operation,payload,createdAt,attempts,acknowledgedAt?. Server có Membership, DeviceSession, ProcessedEvent, SyncChange và Subscription/Entitlement theo task SaaS. Provider OAuth tokens được quản lý bởi SDK/secret store, không nằm trong event hoặc backup logical.

## Interfaces cần ổn định

Tên có thể điều chỉnh trong FND-02 một lần và ghi mapping, các task sau không tự đổi public contract:

```kotlin
interface FinanceRepository {
    fun observeAccounts(workspaceId: WorkspaceId): Flow<List<Account>>
    suspend fun post(command: PostFinanceCommand): PostResult
    suspend fun report(query: FinanceReportQuery): FinanceReport
}
interface WorkMailConnector {
    suspend fun scan(request: MailScanRequest): MailScanPage
}
interface CalendarConnector {
    suspend fun readWindow(request: CalendarWindowRequest): CalendarPage
}
interface NewsConnector {
    suspend fun collect(request: NewsWindowRequest): NewsCollection
}
interface BriefingService {
    suspend fun prepare(workspaceId: WorkspaceId, date: LocalDate): BriefingRun
}
interface AiProvider {
    suspend fun propose(request: AiRequest): AiResponse
}
```

Kotlin data types thực phải được task sở hữu khai báo; không để Any/map tùy tiện xuyên tất cả layer. Interface domain không nhận Context hoặc Retrofit/MSAL type. Side-effect commands có operationId và trả error chuẩn; query không sửa dữ liệu.

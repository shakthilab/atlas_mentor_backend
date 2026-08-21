x# In-App Notifications — Plan & Situation Catalog

Status: **idea / planning doc only** — nothing in this file has been implemented.
Scope: backend design for in-app notifications (the `Notification` entity, `NotificationType`
enum, `NotificationRepository` and `NotificationResponse` DTO already exist in the codebase as
scaffolding; there is **no migration for the `notifications` table yet, no service that writes
rows, and no controller/SSE/WebSocket endpoint that serves them to the frontend**. This doc is
the plan for filling that gap.)

---

## 1. What already exists vs. what's missing

| Piece | State |
|---|---|
| `model/Notification.java` | ✅ exists (user, type, referenceId, title, message, isRead, createdAt) |
| `enums/NotificationType.java` | ✅ exists, but only covers Task events (10 values) |
| `repository/NotificationRepository.java` | ✅ exists (list, unread count, mark read/all-read) |
| `dto/NotificationResponse.java` | ✅ exists |
| DB migration for `notifications` table | ❌ missing — needs `V23__add_notifications_table.sql` |
| `NotificationService` (create/dispatch) | ❌ missing |
| `NotificationController` (REST: list/unread-count/mark-read) | ❌ missing |
| Real-time push (WebSocket/SSE) | ❌ missing — polling vs push decision needed (see §6) |
| Hooks into existing services to actually raise notifications | ❌ missing |
| Notification preferences per user (mute/opt-out per type) | ❌ missing (nice-to-have, not v1) |

---

## 2. Design principles

- **One central `NotificationService.notify(...)`** — every module calls this instead of writing
  rows directly, so delivery (in-app row + later push/email) stays consistent in one place.
- **Fire after the DB transaction commits**, not inline mid-transaction — use
  `TransactionSynchronizationManager.registerSynchronization` (or `@TransactionalEventListener(phase = AFTER_COMMIT)`)
  so a notification is never sent for a change that then rolls back.
- **Notify the right audience, not everyone**: mostly 1) the specific assignee/owner of the
  record, and 2) their reporting manager (via `UserReporting`/`ManagerEmployeeHierarchy`) or the
  relevant approver role (Admin), depending on the event. Avoid blasting all admins for
  low-severity events.
- **`referenceId` + `type` must be enough for the frontend to deep-link** (e.g. `TASK_ASSIGNED` +
  `referenceId=taskId` → navigate to `/tasks/{id}`). Keep `referenceId` semantics consistent per
  type (documented in §3 tables below — mostly task/day/payout/dispute/student id).
  Frontend can be its own module.

---

## 3. Notification situations by domain

Each row = one situation → suggested `NotificationType` value, who receives it, and severity
(drives whether it also becomes a toast/push later, not just a silent bell-icon entry).

### 3.1 Tasks (`Task`, `TaskAssignment`, `TaskComment`, `TaskAttachment`, `TaskActivity`)

| Situation | Type (new/existing) | Recipient(s) | Severity |
|---|---|---|---|
| Task assigned to a user | `TASK_ASSIGNED` ✅ | assignee | Normal |
| Task reassigned to someone else | `TASK_ASSIGNED` (reuse) | new assignee; optionally previous assignee informed of removal | Normal |
| Due date/time approaching (e.g. 2h / 1 day before) | `TASK_DUE_SOON` ✅ | assignee | Normal |
| Task becomes overdue (scheduler already marks `OVERDUE`) | `TASK_OVERDUE` ✅ | assignee, and their manager if still overdue after N hours | High |
| Task marked `DONE`/`COMPLETED` | `TASK_COMPLETED` ✅ | task creator / assignedBy / manager awaiting review | Normal |
| Task reaches `VERIFIED` (all reviewers signed off) | `TASK_STATUS_CHANGED` ✅ | assignee | Normal |
| Comment added on a task | `TASK_COMMENTED` ✅ | assignee (if commenter ≠ assignee), watchers | Low |
| @mention inside a task comment | `MENTION` ✅ | mentioned user | Normal |
| Status changed (e.g. `TODO`→`IN_PROGRESS`) | `TASK_STATUS_CHANGED` ✅ | assignee/watchers | Low |
| Priority changed (e.g. → `HIGH`) | `TASK_PRIORITY_CHANGED` ✅ | assignee | Normal |
| Attachment added | `TASK_ATTACHMENT_ADDED` ✅ | assignee (if uploader ≠ assignee) | Low |
| Task escalated (`TaskAction.TASK_ESCALATED`) | **new: `TASK_ESCALATED`** | escalation target (manager) | High |
| Bundle executed and generated tasks for a user (`BundleExecution`) | `BUNDLE_EXECUTED` ✅ | each user who received a generated task | Low |
| Recurring task's next occurrence created | **new: `TASK_RECURRING_GENERATED`** (or reuse `TASK_ASSIGNED`) | assignee | Low |

### 3.2 Day workspace / approvals (`DayWorkspace`, `DayApproval`, `TaskApproval`)

| Situation | Type | Recipient(s) | Severity |
|---|---|---|---|
| Employee submits day for approval (moves to first approval stage) | **new: `DAY_SUBMITTED_FOR_APPROVAL`** | first-stage approver (manager) | Normal |
| Day approved at a stage, moves to next stage | **new: `DAY_APPROVAL_STAGE_ADVANCED`** | next approver in chain | Normal |
| Day rejected at any stage | **new: `DAY_REJECTED`** | employee | High |
| Day reaches final `ADMIN_VERIFIED` | **new: `DAY_VERIFIED`** | employee | Normal |
| Individual task approval action recorded (`TaskApproval`) | **new: `TASK_APPROVAL_ACTION`** | task assignee | Normal |
| Streak broken / daily completion below threshold | **new: `DAY_COMPLETION_LOW`** (optional, v2) | employee, manager | Low |

### 3.3 Students / leads (`Student`, `StudentStatusApproval`, `StudentActivity`, `StudentNote`)

| Situation | Type | Recipient(s) | Severity |
|---|---|---|---|
| New lead assigned/imported to a counsellor | **new: `STUDENT_ASSIGNED`** | assigned counsellor | Normal |
| Student status change requested (`StudentStatusApproval` created, `PENDING`) | **new: `STUDENT_STATUS_APPROVAL_REQUESTED`** | approver (manager/admin) | Normal |
| Status change approved | **new: `STUDENT_STATUS_APPROVED`** | requester (counsellor) | Normal |
| Status change rejected | **new: `STUDENT_STATUS_REJECTED`** | requester | High |
| Note/mention added on a student record | **new: `STUDENT_NOTE_ADDED`** | assigned counsellor (if note author differs) | Low |
| Student inactive/no activity for N days (nudge) | **new: `STUDENT_FOLLOWUP_DUE`** (v2, scheduler-driven) | assigned counsellor | Normal |

### 3.4 Payments — student payments & client payouts (`StudentPayment`, `ClientPayout`, `PaymentTransaction`, `PaymentDisputeActivity`, `PaymentAmountChange`, `FinancialAuditLog`)

| Situation | Type | Recipient(s) | Severity |
|---|---|---|---|
| Student payment recorded/updated | **new: `STUDENT_PAYMENT_RECORDED`** | assigned counsellor, finance/admin | Normal |
| Student payment status → `REJECTED` | **new: `STUDENT_PAYMENT_REJECTED`** | counsellor | High |
| Student payment → `DISPUTE` | **new: `STUDENT_PAYMENT_DISPUTE`** | admin/finance | High |
| Client payout amount assigned (`ClientPayoutStatus.AMOUNT_ASSIGNED`) | **new: `PAYOUT_AMOUNT_ASSIGNED`** | referral/partner receiving payout | Normal |
| Payment added toward payout (`PARTIAL_PAID`/`PAID`) | **new: `PAYOUT_PAYMENT_ADDED`** | referral/partner | Normal |
| Dispute initiated on a payout (`DISPUTE_INITIATED`) | **new: `PAYOUT_DISPUTE_INITIATED`** | admin (to resolve), and the other party | High |
| Dispute accepted/rejected (`DISPUTE_ACCEPTED`/`DISPUTE_REJECTED`) | **new: `PAYOUT_DISPUTE_RESOLVED`** | referral/partner who raised it | High |
| Payment amount changed after the fact (`PaymentAmountChange`) | **new: `PAYMENT_AMOUNT_CHANGED`** | affected party + admin (audit visibility) | Normal |
| Sensitive financial action logged (`FinancialAuditLog`, e.g. large manual override) | **new: `FINANCIAL_AUDIT_ALERT`** | admin only | High |

### 3.5 Referrals (`ReferralAssignment`, `ReferralDetails`, `ReferralResource`)

| Situation | Type | Recipient(s) | Severity |
|---|---|---|---|
| A referral/lead is assigned to a user | **new: `REFERRAL_ASSIGNED`** | assignee | Normal |
| A referral resource is shared/added for a user | **new: `REFERRAL_RESOURCE_SHARED`** | owner(s) | Low |

### 3.6 Weekly accountability (`WeeklyAccountabilityAssignment`, `WeeklyAccountabilityResponse`, `WeeklyAccountabilityWeek`)

| Situation | Type | Recipient(s) | Severity |
|---|---|---|---|
| New weekly accountability assignment created for employee | **new: `WEEKLY_CHECKIN_ASSIGNED`** | employee | Normal |
| Checkpoint due soon / not yet answered | **new: `WEEKLY_CHECKIN_DUE_SOON`** | employee | Normal |
| Employee submits a response | **new: `WEEKLY_CHECKIN_SUBMITTED`** | manager | Low |
| Missed checkpoint (past due, unanswered) | **new: `WEEKLY_CHECKIN_MISSED`** | employee, manager | High |

### 3.7 Hierarchy / people / roles (`UserReporting`, `ManagerEmployeeHierarchy`, `CounsellorHierarchy`, `Role`, `User`)

| Situation | Type | Recipient(s) | Severity |
|---|---|---|---|
| A new employee is assigned to a manager's reporting line | **new: `HIERARCHY_TEAM_MEMBER_ADDED`** | manager | Low |
| A user's role changes | **new: `ROLE_CHANGED`** | affected user | Normal |
| A user account is deactivated/reactivated (`UserStatus`) | **new: `ACCOUNT_STATUS_CHANGED`** | affected user | High |

### 3.8 System / scheduler-driven

| Situation | Type | Recipient(s) | Severity |
|---|---|---|---|
| `OverdueTaskSchedulerService` sweep marks tasks overdue | `TASK_OVERDUE` ✅ | assignee | High |
| `TemplateInstantiationSchedulerService` runs and creates a day/tasks for a template | already covered by `TASK_ASSIGNED`/`BUNDLE_EXECUTED` | employee | Low |
| `BundleSchedulerService` execution fails (`executionStatus = FAILED`) | **new: `BUNDLE_EXECUTION_FAILED`** | admin only (ops alert) | High |
| `WeeklyAccountabilityAssignmentSchedulerService` fails to generate assignments | **new: `SYSTEM_ALERT`** | admin only | High |

---

## 4. Priority tiers (for later UI treatment — badge color, toast vs. silent)

- **High** — needs attention / something went wrong or is time-sensitive (overdue, rejected,
  dispute, escalation, account/status change, financial audit, scheduler failure). Candidate for
  push/email in addition to in-app once that channel exists.
- **Normal** — standard workflow progress (assigned, approved, submitted, status changed).
  In-app bell only.
- **Low** — passive/FYI (comment added, attachment added, resource shared). In-app bell only,
  safe to batch/digest later.

## 5. Recipient resolution helpers needed in `NotificationService`

- `resolveManagerOf(userId)` → via `UserReporting`/`ManagerEmployeeHierarchy` (may already exist
  in `HierarchyService`, reuse it).
- `resolveAdmins()` → all users with `Role.code`/`isEmployee` matching admin, for system/finance
  alerts.
- `resolveTaskWatchers(taskId)` → assignee + assignedBy + anyone in `TaskComment`/mentions.
- All of the above should short-circuit and no-op if actor == recipient (don't notify yourself
  for your own action).

## 6. Delivery mechanism — decision needed

Two viable paths, not mutually exclusive:

1. **Polling (simplest, ship first)**: frontend polls `GET /api/notifications` and
   `GET /api/notifications/unread-count` every N seconds. No infra change. Matches what the
   repository already supports.
2. **Push (better UX, v2)**: WebSocket (Spring `spring-boot-starter-websocket` +
   STOMP) or Server-Sent Events per authenticated user, so the bell updates instantly. Adds
   moving parts (connection management, auth over WS). Recommend building v1 as polling and
   layering push in later without changing the `Notification` data model.

## 7. API surface to add (`NotificationController`, v1)

- `GET /api/notifications?page=&size=` — paginated list, newest first.
- `GET /api/notifications/unread-count`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/read-all`
- (v2) `GET/PUT /api/notifications/preferences` — per-type mute toggle.

## 8. Rollout order (suggested)

1. Migration `V23__add_notifications_table.sql` + confirm entity mapping matches.
2. `NotificationService` with the generic `notify(user, type, referenceId, title, message)` +
   after-commit dispatch.
3. `NotificationController` (polling-based v1) + wire into frontend bell icon.
4. Wire the **High**-severity situations first (task overdue, day rejected, payout dispute,
   student payment rejected/dispute, financial audit, scheduler failures) — these are the ones
   users actually need pushed to them.
5. Wire remaining **Normal**/**Low** situations.
6. Extend `NotificationType` enum incrementally as each situation is wired (don't pre-add unused
   values to avoid an enum full of dead code).
7. (v2) Preferences + push channel.

---

*This file is a planning document only — no code has been changed as part of writing it.*

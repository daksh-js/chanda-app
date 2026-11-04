# Security Specification — Chanda App (Private Akhada Donation Management System)

## 1. Data Invariants
1. **Strict Authentication & Ownership Isolation**: Every read (`get`, `list`) and write (`create`, `update`, `delete`) across `donors`, `donations`, `receipts`, `authorized_devices`, `activity_logs`, and `app_settings` requires `request.auth != null` and `userId == request.auth.uid`.
2. **CASH-Only Payment Enforcement**: Every `donations` and `receipts` document must strictly have `paymentMethod == 'CASH'`. Any payload specifying UPI, Bank Transfer, Cheque, Card, or Online Payment is rejected at the security rules level.
3. **Immutable Audit Trail**: `createdAt`, `createdBy`, and `userId` are immutable on all updates (`incoming().field == existing().field`). Financial records (`donations`, `receipts`) and `activity_logs` cannot be hard-deleted by clients (`allow delete: if false;`); corrections/reversals update `status` and `correctionNote` while preserving full audit history.
4. **Device Cap & Role Enforcement**: `authorized_devices` enforces valid `role in ['OWNER_ADMIN', 'STAFF_ADMIN']` and `status in ['APPROVED', 'PENDING', 'REVOKED']`. `app_settings` enforces `maxAuthorizedDevices >= 1 && maxAuthorizedDevices <= 6`.
5. **Temporal & Size Bounds**: Every string field is bounded by `.size()`, every document ID path variable is validated via `isValidId()`, and timestamps enforce `is timestamp && <= request.time`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Scraping**: Unauthenticated `get` or `list` on `/donors` or `/donations` -> Rejected (`request.auth != null`).
2. **Cross-User PII Leak**: Authenticated user `bob_456` attempts `get` or `list` on `alice_123`'s `/donors/{id}` -> Rejected (`resource.data.userId == request.auth.uid`).
3. **Unfiltered Query Scraping**: Authenticated user queries `/donors` without `.where("userId", "==", uid)` -> Rejected by `allow list` rule checking `resource.data.userId == request.auth.uid`.
4. **Non-Cash Payment Injection**: Creating a donation with `paymentMethod: "UPI"` or `"BANK_TRANSFER"` -> Rejected (`data.paymentMethod == 'CASH'`).
5. **Negative or Overflow Donation Amount**: Creating a donation with `amount: -500` or `amount: 0` -> Rejected (`data.amount is int && data.amount > 0`).
6. **Shadow Field Injection ("Ghost Field")**: Sending `isSuperAdmin: true` in `/donors` or `/donations` -> Rejected by `incoming().keys().hasOnly(...)` and `affectedKeys().hasOnly(...)`.
7. **Identity Spoofing on Create**: User `alice_123` creates a donor or donation with `userId: "bob_456"` -> Rejected (`incoming().userId == request.auth.uid`).
8. **Ownership Hijack on Update**: Updating `userId` or `createdBy` on an existing donor/donation -> Rejected (`incoming().userId == existing().userId`).
9. **Timestamp Manipulation / Clock Skew**: Writing a non-timestamp or future timestamp to `createdAt` -> Rejected (`incoming().createdAt is timestamp && incoming().createdAt <= request.time`).
10. **Silent Financial Record Deletion**: Attempting `delete` on `/donations/{id}`, `/receipts/{id}`, or `/activity_logs/{id}` -> Rejected (`allow delete: if false;`).
11. **Oversized String DoS ("Denial of Wallet")**: Writing a 10,000-character `fullName` or `notes` -> Rejected by strict `.size()` bounds.
12. **Invalid Device Role Escalation**: Writing `role: "SUPER_ROOT"` or `maxAuthorizedDevices: 99` -> Rejected by enum and numeric range checks.

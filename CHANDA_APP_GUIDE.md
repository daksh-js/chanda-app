# CHANDA APP — Private Akhada Donation Management System

## 1. Architecture & Security Overview
- **Application Name**: Chanda App (`com.aistudio.chandaapp.akhd92`)
- **Access Policy**: Strictly private Admin-Only Akhada ledger. Zero public access, zero public registration, zero donor login.
- **Payment Mode Policy**: Strictly **CASH ONLY (केवल नकद)**. Digital/UPI/Bank/Cheque payment modes are blocked both in the UI and at the Firestore Security Rules layer (`paymentMethod == 'CASH'`).
- **Authentication & Device Enrollment**:
  - Uses Jetpack `CredentialManager` with Google Sign-In (`GetSignInWithGoogleOption` interactive + `GetGoogleIdOption` silent auto-sign-in on startup).
  - Once an authorized device is signed in, subsequent launches bypass sign-in prompts automatically and open directly on the **Home Dashboard**.
  - Supports up to **6 Authorized Administrator Devices** (`OWNER_ADMIN` and `STAFF_ADMIN` roles) tracked in the `authorized_devices` collection with unique installation IDs (`AKH-XXXXXXXX`).
  - If an Owner Admin revokes a device (`status = "REVOKED"`), the revoked device is immediately blocked from viewing or modifying Akhada records.

## 2. Cloud Firestore Collections & Schema
All collections are provisioned in the dedicated Firestore Enterprise database (`R.string.firestore_database_id`) and protected by zero-trust `firestore.rules`:
1. **`donors` (`/donors/{donorId}`)**:
   - Unique `donorCode` (`DONOR-0001`, `DONOR-0002`, ...), `fullName`, `fatherName`, `mobileNumber`, `whatsappNumber`, `alternateMobile`, `address`, `village`, `city`, `district`, `state`, `pinCode`, `occupation`, `birthday`, `notes`, `photoUri`, `addedByAdmin`, `lifetimeDonation`, `donationCount`, `isFavorite`, `consentGiven`, `createdBy`, `updatedBy`, `createdAt`, `updatedAt`.
2. **`donations` (`/donations/{donationId}`)**:
   - `donorId`, `donorCode`, `donorName`, `donorMobile`, `amount` (INR integer > 0), `amountInWords`, `donationDate`, `purpose`, `paymentMethod` (`"CASH"`), `receiptNumber` (`AKD-YYYY-XXXX`), `receivedByAdmin`, `notes`, `status` (`CONFIRMED`, `CORRECTED`, `REVERSED`), `correctionNote`, `createdBy`, `updatedBy`, `createdAt`, `updatedAt`.
   - Financial records cannot be silently deleted (`allow delete: if false;`). Corrections and reversals require an explicit audit reason.
3. **`receipts` (`/receipts/{receiptId}`)**:
   - Stores generated receipt metadata and `qrPayload` for instant PDF regeneration, printing, and WhatsApp sharing.
4. **`authorized_devices` (`/authorized_devices/{deviceId}`)**:
   - Tracks `installationId`, `deviceName`, `adminName`, `role` (`OWNER_ADMIN` / `STAFF_ADMIN`), `status` (`APPROVED`, `PENDING`, `REVOKED`), granular permissions (`canAddDonor`, `canEditDonor`, `canDeleteDonor`, `canRecordDonation`, `canViewReports`, `canExportData`), and `lastActiveAt`.
5. **`activity_logs` (`/activity_logs/{logId}`)**:
   - Append-only audit log (`allow update, delete: if false;`) recording every donor addition/update, cash donation, receipt generation, correction/reversal, and device authorization change.
6. **`app_settings` (`/app_settings/{settingId}`)**:
   - Stores Akhada Name, Registration Number, Official Address, Owner Admin details, Vault Allocation ID, Active Season, Language (`BILINGUAL`, `HI`, `EN`), Dark/Light theme preference, and customizable Hindi/English WhatsApp templates.

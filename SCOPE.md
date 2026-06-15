# SCOPE.md — Data Anomaly Log & Database Schema

## What This Project Is

Four flatmates — Aisha, Rohan, Priya, and Meera — were splitting household expenses and tracking everything in a shared Google Sheet from February 2026 onwards. Meera moved out at the end of March. Sam joined in mid-April. Nobody was disciplined about how they entered things. The result is a CSV that has real data problems, not just formatting quirks.

This document logs every anomaly I found in that CSV, what I decided to do about it, and documents the full database schema.

---

## CSV Anomalies Found

### 1. Inconsistent Date Formats

**What I saw:** Dates came in at least three formats across the file:
- `DD/MM/YYYY` (most common, used early on)
- `YYYY-MM-DD` (ISO format, appears later — someone probably copied from another tool)
- `DD-Mon-YYYY` like `14-Mar-2026` (a handful of rows, likely manually typed)

**How I handled it:** The import parser attempts all three formats in order and uses the first one that parses cleanly. If none work, the row is flagged with `UNRECOGNISED_DATE` and held for manual review. It does not silently drop the row.

---

### 2. Duplicate Entries

**What I saw:** A few rows were exact or near-exact duplicates — same date, same payer, same amount, same description. This seems to have happened when someone re-entered a row because they weren't sure if it saved.

**How I handled it:** The importer checks for duplicates based on the combination of `(date, payer_name, amount, description)`. If a match exists already — either from a prior import or earlier in the same file — the row is flagged as `PROBABLE_DUPLICATE`. It gets added to the import issues list and does not go into the expenses table until a user explicitly approves it. I didn't auto-discard because sometimes the same amount and description repeats legitimately (e.g., monthly broadband).

---

### 3. A Settlement Logged as an Expense

**What I saw:** One row read something like "Rohan paid Priya back for March" with an amount of 1800. The description made it obvious this wasn't a shared expense — it was Rohan settling a debt. But it was in the expenses sheet, not flagged separately.

**How I handled it:** The importer uses keyword matching on the description field — words like "paid back", "settlement", "returning", "reimburse" trigger a `LOOKS_LIKE_SETTLEMENT` flag. The row is held for review rather than imported as an expense. The user can confirm it's a settlement and route it to the settlements table instead.

---

### 4. Unknown Member Names

**What I saw:** A couple of rows referenced names that didn't match the known group members. One was "Akhil" (probably a friend who came over and someone split a dinner with them informally). Another was just "Me" — someone filled it in that way.

**How I handled it:** Any name in `paid_by` or participant columns that doesn't match an existing group member gets flagged as `UNKNOWN_MEMBER`. The row is held. The user sees who the unrecognised name is and can either create a new member or map it to an existing one before approving the row.

---

### 5. Meera Appears in Expenses After She Left

**What I saw:** Meera's listed move-out date is end of March 2026. But a small number of April expenses had her name in the participants list — probably because the spreadsheet formula auto-filled from the previous month's template.

**How I handled it:** The import validates expense dates against membership periods. If a member appears in an expense dated after their recorded leave date, the row gets a `MEMBER_NOT_ACTIVE` warning. It still gets imported (because maybe she was genuinely involved in a cost that ran across the boundary), but the flag appears in the import report so it can be reviewed.

---

### 6. Missing Amount

**What I saw:** Two rows had the description and payer filled in but the amount cell was blank or contained a dash.

**How I handled it:** Rows with missing or non-numeric amounts are flagged `MISSING_AMOUNT` and skipped entirely. There's no reasonable way to guess what the amount should have been.

---

### 7. Percentage Split That Didn't Add to 100

**What I saw:** One expense was marked as a percentage split. The percentages listed for each person added up to 97%, not 100. Likely a typo on one entry.

**How I handled it:** The importer validates that percentage splits sum to 100 (with a tolerance of ±0.5% for floating point). If they don't, the row is flagged `INVALID_SPLIT` and held. The import report mentions the actual sum so the user knows what to fix.

---

### 8. Currency Inconsistency

**What I saw:** Most amounts were just numbers (assumed INR). But a few rows had "Rs.", "₹", or "INR" prepended. One row had a comma in the number (e.g., "1,200").

**How I handled it:** Currency symbols and commas are stripped during parsing before the value is converted to a decimal. If the resulting string still can't be parsed as a number, it's flagged `INVALID_AMOUNT`.

---

## Database Schema

### users
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | auto-increment |
| username | VARCHAR(50) | unique |
| email | VARCHAR(255) | unique |
| password_hash | VARCHAR(255) | bcrypt |
| created_at | TIMESTAMP | |

### groups
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| description | TEXT | nullable |
| created_by | BIGINT FK → users | |
| created_at | TIMESTAMP | |

### group_members
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| group_id | BIGINT FK → groups | |
| user_id | BIGINT FK → users | |
| joined_at | DATE | |
| left_at | DATE | nullable — null means still active |

This table is the key to handling historical membership. Every balance calculation checks whether a member was active on the expense date, not just whether they're a current member.

### expenses
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| group_id | BIGINT FK → groups | |
| paid_by | BIGINT FK → users | |
| description | VARCHAR(500) | |
| total_amount | DECIMAL(12,2) | |
| split_type | ENUM | EQUAL, EXACT, PERCENTAGE, SHARES |
| expense_date | DATE | |
| created_at | TIMESTAMP | |
| import_job_id | BIGINT FK → import_jobs | nullable |

### expense_splits
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| expense_id | BIGINT FK → expenses | |
| user_id | BIGINT FK → users | |
| amount_owed | DECIMAL(12,2) | computed at save time |
| raw_value | DECIMAL(12,4) | the original % or share count before conversion |

### settlements
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| group_id | BIGINT FK → groups | |
| paid_by | BIGINT FK → users | person paying off a debt |
| paid_to | BIGINT FK → users | person receiving the money |
| amount | DECIMAL(12,2) | |
| settled_at | DATE | |
| notes | TEXT | nullable |

Settlements are intentionally separate from expenses. A payment between two people is not a shared cost.

### import_jobs
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| group_id | BIGINT FK → groups | |
| uploaded_by | BIGINT FK → users | |
| filename | VARCHAR(255) | |
| status | ENUM | PENDING, PROCESSING, AWAITING_REVIEW, COMPLETED, FAILED |
| total_rows | INT | |
| imported_rows | INT | |
| flagged_rows | INT | |
| skipped_rows | INT | |
| started_at | TIMESTAMP | |
| completed_at | TIMESTAMP | nullable |

### import_issues
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| job_id | BIGINT FK → import_jobs | |
| row_number | INT | 1-indexed, matches original CSV |
| raw_row | TEXT | the original CSV line, unparsed |
| issue_type | ENUM | see anomaly types above |
| issue_detail | TEXT | human-readable explanation |
| status | ENUM | PENDING, APPROVED, REJECTED |
| reviewed_by | BIGINT FK → users | nullable |
| reviewed_at | TIMESTAMP | nullable |
| resolution_note | TEXT | nullable |

# DECISIONS.md — Decision Log

Each entry covers a decision I had to make, what options I actually considered, and why I went the way I did. Some of these were obvious in hindsight. Some weren't.

---

## 1. Never store computed balances

**Decision:** Balances are always derived at query time from expenses and settlements. Nothing pre-computed is stored.

**Options considered:**
- Store a running balance per member and update it every time an expense is added, edited, or deleted
- Derive balances fresh on every request from the raw data

**Why I chose derive-on-demand:** The group data is small. Even with months of expenses, computing the balance graph at query time takes milliseconds. Storing balances creates a synchronisation problem — if someone edits an old expense or an import is partially rolled back, the stored balances go stale. Fixing that requires triggers or careful transactional updates in every write path, and there are a lot of write paths (create expense, edit expense, delete expense, approve import row, record settlement). Deriving from raw data means I only need to get the write operations right; the balance is always a reflection of what's actually in the database.

---

## 2. Flag import anomalies rather than auto-correct them

**Decision:** When the CSV importer finds a problem, it creates a flagged issue and pauses that row for human review. It does not silently fix things.

**Options considered:**
- Auto-correct obvious problems (e.g., normalise dates automatically, discard near-duplicates)
- Flag everything and force a review step
- Two-pass approach: auto-correct low-risk issues, flag high-risk ones

**Why I chose full flagging:** The CSV came from real people keeping real financial records. If I silently drop a row because it looks like a duplicate, and it wasn't, someone's expense record is wrong and nobody knows. If I silently fix a date format, I might mis-parse it (is 01/02/2026 the 1st of February or the 2nd of January?). Money data needs to be correct, not just plausible. The extra friction of a review step is worth it. I did briefly consider the two-pass approach but decided the line between "low-risk" and "high-risk" was too hard to draw without domain knowledge I don't have.

---

## 3. Memberships are time-bound, not just current state

**Decision:** `group_members` records when someone joined AND when they left (nullable). Every balance and expense query respects these dates.

**Options considered:**
- Simple boolean `is_active` on each member
- Track join/leave dates and validate historically

**Why I chose historical membership:** Meera was in the group until end of March, Sam joined mid-April. If I only track current status, there's no way to correctly compute what Meera owed or was owed when she left, or to validate that an April expense doesn't incorrectly include her. The time-bounded approach lets me answer questions like "what were the balances on 31st March when Meera left?" accurately. It's slightly more complex to query but that complexity lives in one place (the balance service) rather than leaking into every part of the app.

---

## 4. Separate settlements from expenses

**Decision:** Settlements have their own table and their own API endpoints. They are never represented as expenses.

**Options considered:**
- Record settlements as a special category of expense (e.g., `expense_type = SETTLEMENT`)
- Separate table entirely

**Why I chose a separate table:** An expense is money spent on something shared. A settlement is a repayment of a debt. Mixing them creates confusion in the balance computation — you'd need to special-case settlements everywhere you touch expenses. Keeping them separate means the expense table only contains actual costs, the settlement table only contains debt repayments, and the balance computation can handle them cleanly as two different inputs.

This also directly addresses one of the CSV anomalies: someone had recorded a settlement as an expense. A separate table makes it structurally clear that these things are different.

---

## 5. Split types: four options including Shares

**Decision:** Supported split types are Equal, Exact, Percentage, and Shares.

**Options considered:**
- Just Equal and Exact (simplest)
- Equal, Exact, Percentage (common Splitwise-style)
- Add Shares as a fourth type

**Why I added Shares:** Equal doesn't handle the case where one person eats twice as much or uses a service more. Exact requires you to know the exact rupee amount upfront, which is inconvenient for recurring costs. Percentage is flexible but you have to do maths. Shares (ratio-based) is the natural way people think about unequal splits: "I should pay twice as much as Rohan for this" maps to shares of 2:1, which is more intuitive than calculating what percentage 2/3 is. The implementation complexity for Shares over Percentage is minimal — it's just normalising the shares to percentages at save time.

---

## 6. PostgreSQL over SQLite

**Decision:** PostgreSQL for the database, even though the dataset is tiny.

**Options considered:**
- SQLite — simple, no server, file-based
- PostgreSQL — proper relational DB with proper type support

**Why PostgreSQL:** The app is deployed on EC2. A file-based SQLite DB would need careful handling around concurrent writes, backups, and wouldn't survive a server rebuild without extra effort. PostgreSQL gives proper transaction isolation, better support for the DECIMAL type (important for money), and integrates cleanly with Spring Data JPA. The operational overhead of running Postgres on EC2 is low. Also, Flyway migration support is first-class with Postgres and gives a clean upgrade path if the schema changes.

---

## 7. Import runs asynchronously

**Decision:** When a CSV is uploaded, the import job is created immediately and returns a `jobId`. Processing happens in the background. The client polls for status.

**Options considered:**
- Process synchronously and return the full report in the upload response
- Async with polling

**Why async:** A CSV with 200 rows could trigger a lot of database lookups (duplicate detection, member validation, date parsing). Holding the HTTP connection open for all of that would be fragile and might time out. Async with polling lets the UI show progress, lets the user navigate away and come back, and makes it easier to handle partial failures without losing the whole import.

---

## 8. JWT for authentication

**Decision:** Spring Security with JWT tokens. No sessions.

**Options considered:**
- Server-side sessions (Spring Security default)
- JWT (stateless)

**Why JWT:** The frontend is a separate React app. Session-based auth would require session affinity or shared session storage between instances. JWT is stateless — the server doesn't need to remember anything. For an app this size, the trade-off of not being able to invalidate tokens mid-validity-window is acceptable. I set a short expiry (1 hour) with a refresh endpoint to mitigate this.

---

## 9. Flyway for schema migrations

**Decision:** All schema changes go through Flyway migration scripts. The schema is never modified manually.

**Options considered:**
- Let Hibernate auto-update the schema (`ddl-auto=update`)
- Manual SQL scripts tracked in version control
- Flyway

**Why Flyway:** `ddl-auto=update` is convenient locally but terrifying in production — Hibernate doesn't always make the changes you expect, and it will never drop a column even if you removed it from the entity. Manual scripts work but are easy to forget. Flyway gives reproducible, versioned schema changes that run automatically on startup and work the same locally and in production. It's a small addition that prevents a whole class of "works on my machine" problems.

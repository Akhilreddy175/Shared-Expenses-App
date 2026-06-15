# AI_USAGE.md — How I Used AI on This Project

## Tools Used

**Claude (Anthropic)** — primary assistant throughout the project. Used for design discussions, debugging Java code, writing Flyway migration scripts, and talking through edge cases in the balance computation logic.

**GitHub Copilot** — used inside IntelliJ for autocomplete while writing boilerplate (entity classes, repository interfaces, test setup). Mostly useful for things I'd written ten times before in other projects.

---

## The Kinds of Things I Actually Asked

Most of my prompts were conversational rather than "generate this whole class for me." A few representative examples:

**Design questions:**
> "If I want to support members leaving and rejoining a group, what's the cleanest way to model that in a relational schema? I need to be able to ask 'was this person a member on this date'."

This led to the time-bounded `group_members` table design. The AI walked me through the options (boolean flag vs. date range vs. separate history table) and I picked the date range approach after understanding the trade-offs.

**Debugging:**
> "My percentage split validation is passing when the percentages add up to 97. I have a tolerance check but it's not catching it — here's the code."

Paste of the relevant service method. The AI spotted that I'd written `Math.abs(sum - 100) < 0.5` but `sum` was a `float` and the accumulation error over five values was just barely under my tolerance. Switching to `BigDecimal` for the sum fixed it.

**Flyway script help:**
> "Write a Flyway migration to add a `left_at` column to `group_members`, nullable, DATE type. It should default to null for existing rows."

This saved me time on migration syntax I don't write often enough to have memorised.

**CSV edge cases:**
> "What are realistic data problems I'd expect in a spreadsheet that four people have been filling in for two months without any rules?"

This actually produced a useful brainstorm. The AI listed things like inconsistent date formats, duplicate entries, amount fields with currency symbols, names spelled differently by different people, and rows where someone used the expense sheet to note a payment. About half of these I'd already thought of; a few (like the "settlement as expense" one) I hadn't explicitly named even though I knew it could happen.

---

## Three Cases Where the AI Was Wrong

### Case 1: The Balance Algorithm

**What I asked for:** A way to compute the simplified "who owes whom" from a list of individual balances (e.g., if Aisha is owed 500 and Rohan owes 300 and Priya owes 200, get the minimum transfers to settle).

**What the AI gave me:** The greedy algorithm where you repeatedly match the largest creditor with the largest debtor. It explained this as "minimising the number of transactions."

**Why it was wrong:** The algorithm it gave me worked correctly in most cases but had a subtle bug when balances were very close in value due to floating-point rounding. If Rohan's debt was 299.999... and Priya's credit was 300.001..., the algorithm would sometimes produce a tiny residual "ghost" transaction of 0.002 that shouldn't exist.

**How I caught it:** I wrote unit tests for the balance simplification with amounts that came from percentage splits (which produce lots of repeating decimals). One test had a residual transaction of ₹0.01 that shouldn't have been there.

**What I changed:** I added a minimum threshold — any suggested settlement under ₹1 is suppressed. I also moved all balance arithmetic to `BigDecimal` with explicit rounding (HALF_UP, 2 decimal places) at every step rather than accumulating `double` values. The AI hadn't suggested this because I hadn't told it I was dealing with percentage splits.

---

### Case 2: The Duplicate Detection Logic

**What I asked for:** A query to detect if an expense being imported already exists in the database.

**What the AI gave me:** A JPQL query matching on `(date, paidBy, totalAmount, description)` — all four fields must match exactly.

**Why it was wrong:** The description match was case-sensitive and whitespace-sensitive. "Groceries from D-Mart" and "groceries from D-Mart" would not be caught as duplicates. In practice the spreadsheet had inconsistent capitalisation throughout (some rows title-case, some lower-case, some all-caps).

**How I caught it:** I ran the importer against the actual CSV and the duplicate row I knew was in there sailed through without being flagged. I checked the query, realised the issue, and went back to the AI to ask about case-insensitive matching in JPQL.

**What I changed:** Changed the description comparison to use `LOWER()` on both sides in the query, and also trim whitespace from both values before comparison. I also relaxed the amount match to allow ±₹0.50 tolerance because one duplicate had been re-entered with a slightly rounded amount.

---

### Case 3: The JWT Secret Handling

**What I asked for:** How to configure a JWT secret in Spring Boot using environment variables so the value doesn't end up in source control.

**What the AI gave me:** Configuration using `@Value("${jwt.secret}")` reading from `application.yml`, with an example `application.yml` that included `jwt.secret: my-secret-key-here` as a placeholder.

**Why it was wrong:** The example had the placeholder in the main `application.yml` which I was tracking in git. I almost committed it that way. More importantly, the AI's suggested secret was only 14 characters — `my-secret-key-here` is 18 characters — but JJWT 0.12 with HS256 requires the secret to be at minimum 32 bytes. Signing would silently fail at runtime with a key that's too short.

**How I caught it:** When I first ran the app and tried to log in, I got a `WeakKeyException` from JJWT. The error message mentioned the minimum key length, which led me to the problem. I also noticed the placeholder was in the committed `application.yml` before I'd pushed — caught it while reviewing `git diff` before pushing.

**What I changed:** Moved the secret to an environment variable only, with no default value in `application.yml`. Added a startup check (`@PostConstruct`) that validates the secret is at least 32 characters and throws a descriptive exception if it isn't, so it fails fast rather than failing on first login. Added `application.yml` entry as `jwt.secret: ${JWT_SECRET}` with no fallback, so the app won't start at all without the env var set.

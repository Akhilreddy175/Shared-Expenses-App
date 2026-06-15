# Shared Expenses App

A backend REST API for tracking shared household expenses, built to handle real-world messy data.

Four flatmates — Aisha, Rohan, Priya, and Meera — were splitting household costs and logging everything in a shared spreadsheet since February 2026. Meera moved out end of March. Sam joined mid-April. Nobody was consistent about date formats or whether to capitalise things. This app imports that history, flags every problem it finds, and lets you resolve things manually rather than silently guessing.

**Live app:** [https://expenses.akhilreddy.dev](https://expenses.akhilreddy.dev) *(replace with your actual URL)*  
**Repository:** [https://github.com/Akhilreddy175/Shared-Expenses-App](https://github.com/Akhilreddy175/Shared-Expenses-App)

---

## What it does

- Groups with historical membership — members can join and leave, and the system remembers when
- Four split types: Equal, Exact, Percentage, and Shares (ratio-based)
- Balances are always computed fresh from raw data, never stored
- Settlements (debt repayments) are tracked separately from expenses
- CSV import with a review step — every anomaly is surfaced, nothing is silently corrected

---

## AI Used

**Claude (Anthropic)** — used for schema design discussions, debugging, and talking through edge cases in the balance logic & for documantation. See [AI_USAGE.md](AI_USAGE.md) for specifics including three cases where it was wrong and what was changed.

**GitHub Copilot** — autocomplete while writing boilerplate in IntelliJ.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT 0.12) |
| Database | PostgreSQL 14+ |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| CSV parsing | OpenCSV |
| Build | Maven |
| Frontend | React (Vite) |
| Deployment | AWS EC2 + Nginx |

---

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 14+
- Node.js 18+ (for the frontend)

---

## Local Setup

### 1. Clone the repo

```bash
git clone https://github.com/Akhilreddy175/Shared-Expenses-App.git
cd Shared-Expenses-App
```

### 2. Create the database

```sql
CREATE DATABASE shared_expenses;
```

### 3. Set environment variables

The app won't start without these. There are no insecure defaults.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/shared_expenses
export DB_USERNAME=postgres
export DB_PASSWORD=your_db_password
export JWT_SECRET=your-secret-key-minimum-32-characters-long
```

The JWT secret must be at least 32 characters. The app will throw a clear error on startup if it's shorter.

### 4. Run the backend

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. Flyway will create all the tables automatically on first run.

### 5. Check it's working

```bash
curl http://localhost:8080/api/health
```

### 6. Run the frontend (separate terminal)

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`.

---

## Running Tests

```bash
mvn test
```

Tests use an H2 in-memory database — no Postgres needed to run the test suite. Tests cover:

- Balance computation including edge cases with percentage splits
- Split validation (percentages summing to 100, shares being positive)
- CSV import anomaly detection
- Membership date boundary checks

---

## API Overview

All endpoints require a `Bearer` token in the `Authorization` header (except `/api/auth/*`).

All responses follow:

```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-06-01T10:00:00"
}
```

### Authentication

| Method | Path | What it does |
|---|---|---|
| POST | /api/auth/register | Create an account |
| POST | /api/auth/login | Get a JWT token |
| GET | /api/auth/me | Who am I |

### Groups

| Method | Path | What it does |
|---|---|---|
| POST | /api/groups | Create a group |
| GET | /api/groups | My groups |
| GET | /api/groups/{id} | Group details |
| POST | /api/groups/{id}/members | Add a member |
| DELETE | /api/groups/{id}/members/{userId} | Remove a member (sets leave date) |
| GET | /api/groups/{id}/members | Currently active members |
| GET | /api/groups/{id}/members/history | Full membership history including past members |

### Expenses

| Method | Path | What it does |
|---|---|---|
| POST | /api/groups/{groupId}/expenses | Add an expense |
| GET | /api/groups/{groupId}/expenses | List expenses |
| GET | /api/groups/{groupId}/expenses/{id} | Get one expense |
| PUT | /api/groups/{groupId}/expenses/{id} | Edit an expense |
| DELETE | /api/groups/{groupId}/expenses/{id} | Delete an expense |

### Balances

| Method | Path | What it does |
|---|---|---|
| GET | /api/groups/{groupId}/balances | Full balance summary for the group |
| GET | /api/groups/{groupId}/balances/settlements | Simplified who-owes-whom (minimum transactions) |
| GET | /api/groups/{groupId}/balances/me | Just my balance |

### Settlements

| Method | Path | What it does |
|---|---|---|
| POST | /api/groups/{groupId}/settlements | Record that someone paid someone back |
| GET | /api/groups/{groupId}/settlements | Settlement history |

### CSV Import

| Method | Path | What it does |
|---|---|---|
| POST | /api/groups/{groupId}/imports | Upload a CSV file |
| GET | /api/groups/{groupId}/imports/{jobId} | Check import status |
| GET | /api/groups/{groupId}/imports/{jobId}/issues | List flagged rows needing review |
| POST | /api/groups/{groupId}/imports/{jobId}/issues/{issueId}/review | Approve or reject a flagged row |
| GET | /api/groups/{groupId}/imports/{jobId}/report | Download the full import report |

---

## CSV Format

The importer accepts CSV files with these columns:

```
date, paid_by, description, amount, split_type, participants
```

`participants` is optional for equal splits (defaults to all active members on the expense date). For other split types, format is `Name:value` pairs separated by `/`.

The importer handles inconsistent date formats, currency symbols in amount fields, and mixed casing on names. Everything it can't resolve automatically gets flagged for review.

---

## Project Documentation

- [SCOPE.md](SCOPE.md) — data anomaly log and full database schema
- [DECISIONS.md](DECISIONS.md) — decision log: what was considered, what was chosen, and why
- [AI_USAGE.md](AI_USAGE.md) — AI tools used and three concrete cases where the AI was wrong
- Import report — generated per import job, available at `/api/groups/{groupId}/imports/{jobId}/report`

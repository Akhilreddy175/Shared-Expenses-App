# Shared Expenses App

A backend REST API for tracking shared household expenses. Built as an internship evaluation project.

Four flat mates — Aisha, Rohan, Priya, and Meera — have been tracking expenses in a spreadsheet since February 2026. Meera moved out at the end of March, Sam joined mid-April. The spreadsheet is messy: inconsistent date formats, duplicate entries, a settlement logged as an expense, unknown members, and more. This app solves that.

## What it does

- Manages groups where members can join and leave over time (membership is historical)
- Tracks shared expenses with four split types: Equal, Exact, Percentage, and Shares
- Computes balances on demand — never stores them, always derives them from the actual expense data
- Records settlements separately from expenses (a payment is not an expense)
- Imports the messy CSV, surfaces every problem it finds, and lets the user decide what to do — no silent corrections

## Architecture

Monolithic Spring Boot application using a straightforward layered architecture:

```
Controller → Service → Repository → Database
```

No microservices, no message queues, no event sourcing. The goal is a system that works, is easy to reason about, and can be explained line-by-line in an interview.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT 0.12) |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| CSV Parsing | OpenCSV |
| Build | Maven |
| Deployment | AWS EC2 + Nginx |

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 14+

## Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd shared-expenses-app
   ```

2. **Create the database**
   ```sql
   CREATE DATABASE shared_expenses;
   ```

3. **Configure environment variables** (or update `application.yml` for local dev)
   ```bash
   export DB_USERNAME=postgres
   export DB_PASSWORD=yourpassword
   export JWT_SECRET=your-secret-key-at-least-32-chars
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

5. **Verify it's running**
   ```
   GET http://localhost:8080/api/health
   ```

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory database — no PostgreSQL required.

## API Documentation

See the sections below. All endpoints return:
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-06-01T10:00:00"
}
```

### Authentication
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/auth/register | Create account |
| POST | /api/auth/login | Get JWT token |
| GET | /api/auth/me | Get current user |

### Groups
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/groups | Create group |
| GET | /api/groups | My groups |
| GET | /api/groups/{id} | Group details |
| POST | /api/groups/{id}/members | Add member |
| DELETE | /api/groups/{id}/members/{userId} | Remove member |
| GET | /api/groups/{id}/members | Active members |
| GET | /api/groups/{id}/members/history | Membership history |

### Expenses
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/groups/{groupId}/expenses | Create expense |
| GET | /api/groups/{groupId}/expenses | List expenses |
| GET | /api/groups/{groupId}/expenses/{id} | Get expense |
| PUT | /api/groups/{groupId}/expenses/{id} | Update expense |
| DELETE | /api/groups/{groupId}/expenses/{id} | Delete expense |

### Balances
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/groups/{groupId}/balances | Group balance summary |
| GET | /api/groups/{groupId}/balances/settlements | Simplified who-owes-whom |
| GET | /api/groups/{groupId}/balances/me | My balance |

### Settlements
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/groups/{groupId}/settlements | Record a payment |
| GET | /api/groups/{groupId}/settlements | List settlements |

### Import
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/groups/{groupId}/imports | Upload CSV |
| GET | /api/groups/{groupId}/imports/{jobId} | Import status |
| GET | /api/groups/{groupId}/imports/{jobId}/issues | Flagged rows |
| POST | /api/groups/{groupId}/imports/{jobId}/issues/{issueId}/review | Approve/reject |
| GET | /api/groups/{groupId}/imports/{jobId}/report | Download report |
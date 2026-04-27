# OnlineDataRepository

A full-stack web application for uploading, managing, exploring, and sharing datasets. Built with **Spring Boot 4** (Java 21) on the backend and a **Flask microservice** (Python) for automated metadata extraction from CSV/spreadsheet files.

---

## Overview

OnlineDataRepository (DataHub) is a collaborative platform where users can publish datasets, control who sees them, explore table contents with a built-in query builder, rate and comment on shared data, and receive real-time notifications. An admin dashboard provides platform-wide analytics and audit trails.

---

## Features

**Dataset Management**
- Upload datasets as CSV, TSV, XLSX, or XLS files
- Attach supplementary documents (PDF, DOCX, TXT, MD, RTF)
- Assign tags from a curated catalog: Computer Science, Healthcare, Climate, Economics, Education, Social Science
- Set visibility to `PRIVATE`, `PUBLIC`, or `AUTHORIZED` (per-user access grants)
- Edit dataset metadata and replace or add file versions at any time
- Full version history with change summaries

**Data Exploration**
- Browse and filter all publicly accessible datasets
- View auto-extracted table schema: column names, inferred types, row/column counts
- Query builder with filter support and paginated results (50 rows per page)
- Export query results to CSV (up to 5,000 rows)
- Download original dataset files directly

**Social & Community**
- 5-star rating system per dataset
- Threaded comment system with reply support
- Email notifications for new comments, replies, and ratings on your datasets
- In-app notification centre

**User Accounts**
- Email-based registration and login with BCrypt password hashing
- "Remember me" sessions (24-hour token validity)
- Profile settings: display name, email, password change, account deletion
- Per-user dashboard showing owned datasets and activity

**Admin Panel**
- Platform statistics: total users, total datasets, activity over time
- User activity leaderboard
- Dataset leaderboard by views/downloads
- Full audit log with date-range filtering and CSV export
- Admin-only access enforced by Spring Security

**Automated Metadata Extraction**
- Asynchronous extraction triggered on upload via the Python microservice
- Extracts column names, data types, row counts, and sample statistics
- Extraction status tracked per table (PENDING → SUCCESS / FAILED)

---


## Tech Stack

| Layer | Technology |
|---|---|
| Language (backend) | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Security | Spring Security 6 (form login, BCrypt) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Templating | Thymeleaf + thymeleaf-extras-springsecurity6 |
| Build | Maven (via `mvnw` wrapper) |
| Boilerplate reduction | Lombok |
| Email | Spring Mail (Gmail SMTP / STARTTLS) |
| Async processing | Spring `@Async` with custom thread pool |
| Python service language | Python 3 |
| Python web framework | Flask 2.3.3 |
| Python spreadsheet parsing | openpyxl 3.1.5 |
| Python config | python-dotenv 1.0.0 |


---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 21 or later |
| Maven | 3.9+ (or use the included `mvnw` wrapper) |
| PostgreSQL | 14 or later |
| Python | 3.9 or later |
| pip | latest |

---

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd <repository-folder>
```

### 2. Configure the Database

Create a PostgreSQL database and a dedicated user:

```sql
CREATE DATABASE online_data_repo;
CREATE USER datahub_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE online_data_repo TO datahub_user;
```

Hibernate will auto-create tables on first startup (`spring.jpa.hibernate.ddl-auto=update`). Manual SQL migrations in `src/main/resources/db/migration/` may need to be applied if you are upgrading an existing schema — see [Database Migrations](#database-migrations).

### 3. Configure Application Properties

Copy the example file and fill in your values:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

At minimum, update the following in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/online_data_repo
spring.datasource.username=datahub_user
spring.datasource.password=your_password

spring.mail.username=your-gmail@gmail.com
spring.mail.password=your-gmail-app-password   # Use a Gmail App Password, not your account password

app.mail.from=your-gmail@gmail.com
app.mail.from-name=DataHub
app.base-url=http://localhost:8080

# URL of the Python metadata extraction service
python-service.url=http://localhost:8000
```


### 4. Configure the Python Microservice

```bash
cd python_service
cp .env.example .env
```

Edit `.env`:

```env
PYTHON_SERVICE_PORT=8000
FILE_STORAGE_TYPE=local
```

Install Python dependencies:

```bash
pip install -r requirements.txt
```

### 5. Run the Python Microservice

```bash
cd python_service
python app.py
```

The service starts on `http://localhost:8000`. Verify it is running:

```bash
curl http://localhost:8000/health
# {"status": "ok", "timestamp": "..."}
```

### 6. Run the Spring Boot Application

From the project root:

```bash
./mvnw spring-boot:run
```

Or on Windows:

```cmd
mvn spring-boot:run
```

The application starts on **http://localhost:8080**.

Register a new account at `/register`. The first user account you promote to `ADMIN` role (update the `role` column in the `users` table to `ADMIN`) will be able to access `/admin`.

---


## Key Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/` | Home / redirect |
| GET | `/login` | Login page |
| GET | `/register` | Registration page |
| GET | `/dashboard` | User's personal dashboard |
| GET | `/datasets` | Browse all accessible datasets |
| GET | `/dataset/{id}` | Dataset detail, explorer, comments, ratings |
| GET | `/upload` | Upload wizard (step 1: file; step 2: metadata) |
| POST | `/upload/file` | Submit file upload |
| GET | `/edit/{id}` | Edit an existing dataset |
| GET | `/my-datasets` | Datasets owned by the current user |
| GET | `/query-builder/{id}` | SQL-style query builder for a dataset |
| GET | `/admin` | Admin dashboard (ADMIN role required) |
| GET | `/settings` | User settings |
| GET | `/notifications` | In-app notification list |
| GET | `/api/datasets` | REST API: paginated dataset list |
| GET | `/api/datasets/{id}` | REST API: single dataset summary |
| POST | `/python/health` | Proxied health check of Python service |

---

## File Upload Limits

The application is configured for large uploads out of the box:

| Setting | Value |
|---|---|
| Max file size | 300 MB |
| Max request size | 300 MB |
| In-memory threshold | 2 MB (larger parts spooled to disk) |
| Connection timeout | 300 seconds |
| Async request timeout | 300 seconds |
| Tomcat max swallow size | Unlimited (prevents connection resets on large rejected uploads) |

Allowed **dataset** file types: `csv`, `tsv`, `xlsx`

Allowed **document** attachment types: `pdf`, `txt`, `md`, `doc`, `docx`, `rtf`

---

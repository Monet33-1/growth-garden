# Growth Garden

A small Spring Boot diary and growth-garden application. The backend serves the
REST API and the static single-page frontend from the same process.

## Requirements

- JDK 21
- MySQL 8 for the server deployment
- A DeepSeek API key (optional; AI falls back to “无题” without one)

## Windows desktop profile

The `desktop` profile uses an embedded H2 database, so MySQL is not required.

```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=desktop
```

Open <http://localhost:18080>. On first launch the UI asks for the garden
username, password, nickname, and an optional DeepSeek key.

If the AI service is unavailable, the diary is still saved and the UI asks the
user to choose emotions manually.

The desktop build authorizes its single signed-in account for
`/admin/letters`, so hidden letters can be managed locally. Those edits go
straight into the local H2 database. They are not pushed to other installations;
publish a new version to distribute changed letters.

Desktop data and settings are stored outside the project:

```text
%USERPROFILE%\.growth-garden\
├── data\growth_garden.mv.db
├── config\application-local.properties
└── logs\
```

## Windows desktop build

The Electron shell and packaging instructions live in
[`desktop/electron/README.md`](desktop/electron/README.md).

## Local development

1. Create a MySQL database:

```sql
CREATE DATABASE growth_garden
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

If `db/schema.sql` is present, import it and the emotion seed:

```bash
mysql -u root -p growth_garden < db/schema.sql
mysql -u root -p growth_garden < db/seed-emotions.sql
```

2. Copy the local configuration template:

```bash
copy src\main\resources\application-local.example.properties src\main\resources\application-local.properties
```

3. Fill in `application-local.properties`:

```properties
spring.datasource.password=YOUR_DB_PASSWORD
ai.deepseek.api-key=YOUR_DEEPSEEK_API_KEY
jwt.secret=YOUR_RANDOM_SECRET_OF_AT_LEAST_32_BYTES
app.admin.username=YOUR_ADMIN_USERNAME
app.admin.password-salt=YOUR_BASE64_SALT
app.admin.password-hash=YOUR_BASE64_HASH
```

`application-local.properties` is ignored by git. Never commit it.

4. Start the application:

```bash
.\mvnw.cmd spring-boot:run
```

Open <http://localhost:8080>.

## Exporting a deployment seed

The export scripts write safe schema/emotion files to `db/` and private seed
files to `db/export/`, which is ignored by git:

```powershell
.\scripts\export-db.ps1 -SeedUsername YOUR_USERNAME -UseLocalConfigPassword
```

```bash
SEED_USERNAME=YOUR_USERNAME bash scripts/export-db.sh
```

`-UseLocalConfigPassword` reads the ignored local database password instead of
prompting for it. Without that switch the script prompts with hidden input.

They export:

- `db/schema.sql`
- `db/seed-emotions.sql`
- `db/export/seed-letters.sql`
- `db/export/seed-user.sql`
- `db/export/seed-garden.sql`

`db/export/seed-letters.sql` contains the letter text. Do not publish it if the letters are
private. The scripts deliberately do not export `diary` or `user_letter` rows.

## Production deployment

Use the `prod` profile and provide configuration through environment variables:

| Variable | Meaning |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Set to `prod` |
| `DB_HOST` | MySQL host |
| `DB_PORT` | MySQL port |
| `DB_NAME` | Database name |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |
| `DEEPSEEK_API_KEY` | DeepSeek API key |
| `JWT_SECRET` | Random JWT signing secret, at least 32 bytes |
| `ADMIN_USERNAME` | Admin login name |
| `ADMIN_PASSWORD_SALT` | Base64 PBKDF2 salt |
| `ADMIN_PASSWORD_HASH` | Base64 PBKDF2 hash |
| `PORT` | HTTP port supplied by the platform |

Build the image:

```bash
docker build -t growth-garden .
```

### VPS with Docker Compose

Copy the environment template and fill it in:

```bash
cp .env.example .env
```

Upload the private seed files (`db/export/seed-*.sql`) to the same path on the
server, then start both MySQL and the application:

```bash
docker compose up -d --build
docker compose logs -f app
```

Open `http://SERVER_IP:8080`.

The MySQL init scripts run only when the `mysql-data` volume is first created.
To recreate the database from scratch, remove the volume with
`docker compose down -v` (this deletes all data).

Import the generated SQL files into the production database before the first
login when using an external MySQL:

```bash
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" < db/schema.sql
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" < db/seed-emotions.sql
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" < db/export/seed-letters.sql
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" < db/export/seed-user.sql
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" < db/export/seed-garden.sql
```

Registration is intentionally disabled; create the single account with
`seed-user.sql` and `seed-garden.sql`.

## Security notes

- Rotate any API key or password that has previously been committed or shared.
- Keep `application-local.properties`, `db/export/`, and the admin credentials
  out of git.
- The admin password is stored as a PBKDF2 hash; changing it requires generating
  a new salt and hash.
- `LOCKED` letters are hidden at the API layer, not encrypted in the database.

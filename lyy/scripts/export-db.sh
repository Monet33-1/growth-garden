#!/usr/bin/env bash
# Export schema and safe seed data for a fresh deployment.
# Never exports diary or user_letter rows.
set -euo pipefail

DB_NAME="${DB_NAME:-growth_garden}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
SEED_USERNAME="${SEED_USERNAME:-}"
OUTPUT_DIR="${OUTPUT_DIR:-db/export}"
MYSQLDUMP_BIN="${MYSQLDUMP_BIN:-mysqldump}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

if ! command -v "$MYSQLDUMP_BIN" >/dev/null 2>&1; then
  echo "找不到 mysqldump，請安裝 MySQL Client 或設定 MYSQLDUMP_BIN" >&2
  exit 1
fi
if ! command -v "$MYSQL_BIN" >/dev/null 2>&1; then
  echo "找不到 mysql，請安裝 MySQL Client 或設定 MYSQL_BIN" >&2
  exit 1
fi
if [[ -z "$SEED_USERNAME" ]]; then
  read -r -p "要匯出的唯一帳號 username: " SEED_USERNAME
fi

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_DIR="$PROJECT_ROOT/db"
TARGET_DIR="$PROJECT_ROOT/$OUTPUT_DIR"
mkdir -p "$DB_DIR" "$TARGET_DIR"
echo "警告：schema.sql 預設包含 DROP TABLE，請只匯入全新的資料庫。" >&2
echo "Schema: $DB_DIR"
echo "Seeds : $TARGET_DIR"

read -r -s -p "MySQL password for $DB_USER: " MYSQL_PWD
printf '\n'
export MYSQL_PWD
trap 'unset MYSQL_PWD' EXIT

common=(--host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" --default-character-set=utf8mb4 --single-transaction --skip-comments)
mysql_common=(--host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" --default-character-set=utf8mb4 --database="$DB_NAME")

schema_file="$DB_DIR/schema.sql"
emotions_file="$DB_DIR/seed-emotions.sql"
letters_file="$TARGET_DIR/seed-letters.sql"
user_file="$TARGET_DIR/seed-user.sql"
garden_file="$TARGET_DIR/seed-garden.sql"
rm -f "$schema_file" "$emotions_file" "$letters_file" "$user_file" "$garden_file"

echo "[1/5] schema.sql"
"$MYSQLDUMP_BIN" "${common[@]}" --no-data --routines --triggers --result-file="$schema_file" "$DB_NAME"

echo "[2/5] seed-emotions.sql"
"$MYSQLDUMP_BIN" "${common[@]}" --no-create-info --complete-insert --result-file="$emotions_file" "$DB_NAME" emotion

echo "[3/5] seed-letters.sql"
"$MYSQLDUMP_BIN" "${common[@]}" --no-create-info --complete-insert --result-file="$letters_file" "$DB_NAME" letter

escaped_username="${SEED_USERNAME//\'/\'\'}"
ids_output=$("$MYSQL_BIN" "${mysql_common[@]}" --batch --skip-column-names -e "SELECT id FROM garden_user WHERE username = '$escaped_username'")
ids_output="$(printf '%s\n' "$ids_output" | sed '/^[[:space:]]*$/d')"
count="$(printf '%s\n' "$ids_output" | grep -c . || true)"
if [[ "$count" -ne 1 ]]; then
  echo "找不到唯一的使用者 '$SEED_USERNAME'（查到 $count 筆）" >&2
  exit 1
fi
user_id="$ids_output"

echo "[4/5] seed-user.sql (user id $user_id)"
"$MYSQLDUMP_BIN" "${common[@]}" --no-create-info --complete-insert --where="username='$escaped_username'" --result-file="$user_file" "$DB_NAME" garden_user

echo "[5/5] seed-garden.sql"
"$MYSQLDUMP_BIN" "${common[@]}" --no-create-info --complete-insert --where="user_id=$user_id" --result-file="$garden_file" "$DB_NAME" garden

echo
echo "完成："
ls -lh "$schema_file" "$emotions_file" "$letters_file" "$user_file" "$garden_file"
echo
echo "schema.sql 與 seed-emotions.sql 可提交；db/export 已被 .gitignore 忽略，seed-letters.sql 含信件內容，請勿提交到公開倉庫。"

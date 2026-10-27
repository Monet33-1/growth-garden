#requires -Version 5.1
<#
.SYNOPSIS
  Export the schema and the safe seed data needed by a fresh deployment.

.DESCRIPTION
  Exports the full table structure, the emotion dictionary, the preset letters,
  and exactly one user plus that user's garden. It never exports diary rows or
  user_letter rows.

  The output directory defaults to db\export and is ignored by git. Do not put
  these files in a public repository: seed-letters.sql contains the letter text.

.EXAMPLE
  .\scripts\export-db.ps1 -SeedUsername lyy
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$SeedUsername,

    [string]$Database = "growth_garden",
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$OutputDir = "db\export",
    [string]$MysqlDump = "",
    [string]$Mysql = "",
    [switch]$UseLocalConfigPassword
)

$ErrorActionPreference = "Stop"

function Resolve-Executable {
    param(
        [string]$Configured,
        [string]$CommandName
    )

    if ($Configured) {
        if (-not (Test-Path -LiteralPath $Configured)) {
            throw "找不到 $CommandName：$Configured"
        }
        return (Resolve-Path -LiteralPath $Configured).Path
    }

    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "找不到 $CommandName。請安裝 MySQL Client，或使用 -$($CommandName.Substring(0,1).ToUpper() + $CommandName.Substring(1)) 指定完整路徑。"
    }
    return $command.Source
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$dbDir = Join-Path $projectRoot "db"
$targetDir = Join-Path $projectRoot $OutputDir
$mysqlDumpExe = Resolve-Executable -Configured $MysqlDump -CommandName "mysqldump"
$mysqlExe = Resolve-Executable -Configured $Mysql -CommandName "mysql"

foreach ($directory in @($dbDir, $targetDir)) {
    if (-not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
}

Write-Host "MySQL dump : $mysqlDumpExe"
Write-Host "MySQL      : $mysqlExe"
Write-Host "Database   : $Database"
Write-Host "Seed user  : $SeedUsername"
Write-Host "Schema dir : $dbDir"
Write-Host "Seed dir   : $targetDir"
Write-Host ""
Write-Host "警告：schema.sql 預設包含 DROP TABLE，請只匯入全新的資料庫。" -ForegroundColor Yellow
Write-Host ""

$passwordPointer = [IntPtr]::Zero
$plainPassword = $null

if ($UseLocalConfigPassword) {
    $configPath = Join-Path $projectRoot "src\main\resources\application-local.properties"
    if (-not (Test-Path -LiteralPath $configPath)) {
        throw "找不到 $configPath"
    }
    $passwordLine = Get-Content -LiteralPath $configPath |
        Where-Object { $_ -match '^spring\.datasource\.password=' } |
        Select-Object -First 1
    if (-not $passwordLine) {
        throw "$configPath 裡沒有 spring.datasource.password"
    }
    $plainPassword = $passwordLine -replace '^spring\.datasource\.password=', ''
    Write-Host "使用 application-local.properties 裡的 MySQL 密碼。" -ForegroundColor Cyan
}
else {
    Write-Host "請輸入 MySQL 密碼（輸入時不會顯示），然後按 Enter。" -ForegroundColor Cyan
    $securePassword = Read-Host "MySQL password for $User" -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
}

$previousPassword = $env:MYSQL_PWD

try {
    $env:MYSQL_PWD = $plainPassword

    $common = @(
        "--host=$HostName",
        "--port=$Port",
        "--user=$User",
        "--default-character-set=utf8mb4",
        "--single-transaction",
        "--skip-comments"
    )

    $mysqlCommon = @(
        "--host=$HostName",
        "--port=$Port",
        "--user=$User",
        "--default-character-set=utf8mb4",
        "--database=$Database"
    )

    $schemaFile = Join-Path $dbDir "schema.sql"
    $emotionsFile = Join-Path $dbDir "seed-emotions.sql"
    $lettersFile = Join-Path $targetDir "seed-letters.sql"
    $userFile = Join-Path $targetDir "seed-user.sql"
    $gardenFile = Join-Path $targetDir "seed-garden.sql"

    foreach ($file in @($schemaFile, $emotionsFile, $lettersFile, $userFile, $gardenFile)) {
        if (Test-Path -LiteralPath $file) {
            Remove-Item -LiteralPath $file -Force
        }
    }

    Write-Host "[1/5] schema.sql"
    & $mysqlDumpExe @common --no-data --routines --triggers --result-file=$schemaFile $Database
    if ($LASTEXITCODE -ne 0) { throw "匯出 schema 失敗" }

    Write-Host "[2/5] seed-emotions.sql"
    & $mysqlDumpExe @common --no-create-info --complete-insert --result-file=$emotionsFile $Database emotion
    if ($LASTEXITCODE -ne 0) { throw "匯出情緒資料失敗" }

    Write-Host "[3/5] seed-letters.sql"
    & $mysqlDumpExe @common --no-create-info --complete-insert --result-file=$lettersFile $Database letter
    if ($LASTEXITCODE -ne 0) { throw "匯出信件資料失敗" }

    $escapedUsername = $SeedUsername.Replace("'", "''")
    $ids = @(& $mysqlExe @mysqlCommon --batch --skip-column-names -e "SELECT id FROM garden_user WHERE username = '$escapedUsername'")
    if ($LASTEXITCODE -ne 0) { throw "查詢使用者失敗" }

    $ids = @($ids | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    if ($ids.Count -ne 1) {
        throw "找不到唯一的使用者 '$SeedUsername'（查到 $($ids.Count) 筆）"
    }
    $userId = [long]$ids[0]

    Write-Host "[4/5] seed-user.sql (user id $userId)"
    & $mysqlDumpExe @common --no-create-info --complete-insert --where="username='$escapedUsername'" --result-file=$userFile $Database garden_user
    if ($LASTEXITCODE -ne 0) { throw "匯出使用者失敗" }

    Write-Host "[5/5] seed-garden.sql"
    & $mysqlDumpExe @common --no-create-info --complete-insert --where="user_id=$userId" --result-file=$gardenFile $Database garden
    if ($LASTEXITCODE -ne 0) { throw "匯出花園失敗" }

    Write-Host ""
    Write-Host "完成："
    Get-Item -LiteralPath $schemaFile, $emotionsFile, $lettersFile, $userFile, $gardenFile |
        Select-Object FullName, Length
    Write-Host ""
    Write-Host "schema.sql 與 seed-emotions.sql 可提交；db\export 已被 .gitignore 忽略，seed-letters.sql 含信件內容，請勿提交到公開倉庫。"
}
finally {
    if ($null -eq $previousPassword) {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
    else {
        $env:MYSQL_PWD = $previousPassword
    }
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

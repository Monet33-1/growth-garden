#requires -Version 5.1
<#
.SYNOPSIS
  Converts the private MySQL letter seed into an H2 MERGE script.

.DESCRIPTION
  The generated file is intentionally ignored by git. Keep the source seed in
  db/export/ and only package it into local desktop builds.
#>
param(
    [string]$Source = (Join-Path (Split-Path -Parent $PSScriptRoot) 'db\export\seed-letters.sql'),
    [string]$Output = (Join-Path $PSScriptRoot '..\src\main\resources\db\desktop\letters.sql')
)

$ErrorActionPreference = 'Stop'

$sourcePath = [IO.Path]::GetFullPath($Source)
$outputPath = [IO.Path]::GetFullPath($Output)

if (-not (Test-Path -LiteralPath $sourcePath)) {
    if (Test-Path -LiteralPath $outputPath) {
        Remove-Item -LiteralPath $outputPath -Force
    }
    Write-Warning "Private letter seed not found: $sourcePath"
    Write-Warning 'The desktop build will start without hidden letters.'
    exit 0
}

$raw = Get-Content -LiteralPath $sourcePath -Raw -Encoding UTF8
$match = [regex]::Match(
    $raw,
    '(?s)INSERT\s+INTO\s+`letter`(?:\s*\([^)]*\))?\s*VALUES\s*(.*?);'
)

if (-not $match.Success) {
    throw "Could not find the letter INSERT statement in $sourcePath"
}

$values = $match.Groups[1].Value.Trim()
# H2's MySQL mode does not interpret MySQL backslash-newline escapes.
$values = $values.Replace(([string][char]92 + 'n'), ([string][char]10))
$sql = @"
CREATE TABLE IF NOT EXISTS desktop_letter_seed (
    id BIGINT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(100000) NOT NULL,
    unlock_type VARCHAR(30) NOT NULL,
    unlock_value INT NOT NULL,
    cover VARCHAR(255),
    sort INT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

DELETE FROM desktop_letter_seed;

INSERT INTO desktop_letter_seed
    (id, title, content, unlock_type, unlock_value, cover, sort, create_time, update_time)
VALUES
$values;

INSERT INTO letter
    (id, title, content, unlock_type, unlock_value, cover, sort, create_time, update_time)
SELECT
    s.id, s.title, s.content, s.unlock_type, s.unlock_value,
    s.cover, s.sort, s.create_time, s.update_time
FROM desktop_letter_seed s
WHERE NOT EXISTS (
    SELECT 1 FROM letter l WHERE l.id = s.id
);

DROP TABLE desktop_letter_seed;
"@

New-Item -ItemType Directory -Path (Split-Path -Parent $outputPath) -Force | Out-Null
[IO.File]::WriteAllText($outputPath, $sql, [Text.UTF8Encoding]::new($false))
Write-Host "Desktop letter seed written to: $outputPath"

#requires -Version 5.1
<#
.SYNOPSIS
  Builds the application jar and a small Java runtime for the Windows desktop package.

.EXAMPLE
  .\desktop\build-runtime.ps1
#>
param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$Output = (Join-Path $PSScriptRoot 'runtime')
)

$ErrorActionPreference = 'Stop'

if (-not $JdkHome) {
    throw 'JAVA_HOME is not set. Point it at a JDK 21 installation.'
}

$jlink = Join-Path $JdkHome 'bin\jlink.exe'
if (-not (Test-Path -LiteralPath $jlink)) {
    throw "jlink not found: $jlink"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    Write-Host 'Preparing private desktop letter seed...'
    & (Join-Path $PSScriptRoot 'prepare-letters.ps1')

    Write-Host 'Building application jar...'
    & .\mvnw.cmd -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw 'Maven build failed'
    }
}
finally {
    Pop-Location
}

$resolvedOutput = [IO.Path]::GetFullPath($Output)
$desktopRoot = [IO.Path]::GetFullPath($PSScriptRoot)
if (-not $resolvedOutput.StartsWith($desktopRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to remove output outside desktop/: $resolvedOutput"
}

if (Test-Path -LiteralPath $resolvedOutput) {
    Remove-Item -LiteralPath $resolvedOutput -Recurse -Force
}

Write-Host 'Building Java runtime with jlink...'
$modules = @(
    'java.se',
    'jdk.unsupported',
    'jdk.crypto.ec',
    'jdk.zipfs',
    'jdk.management',
    'jdk.httpserver',
    'jdk.naming.dns'
) -join ','

& $jlink `
    --add-modules $modules `
    --strip-debug `
    --no-header-files `
    --no-man-pages `
    --compress=zip-6 `
    --output $resolvedOutput

if ($LASTEXITCODE -ne 0) {
    throw 'jlink failed'
}

Write-Host "Runtime written to: $resolvedOutput"
Write-Host "Java executable: $resolvedOutput\bin\java.exe"

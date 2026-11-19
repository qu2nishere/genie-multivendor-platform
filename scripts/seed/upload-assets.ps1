<#
  Đẩy ảnh mẫu lên Silo của MÁY DEV — bản Windows PowerShell 5.1 của upload-assets.sh (plan seed 2026-11-16 S04.01, D6).
    web\public\img\products\*  → <tiền tố>product-media/seed/product/
    web\public\img\banners\*   → <tiền tố>content-assets/seed/banner/
  Đọc cùng biến với backend (MINIO_ENDPOINT, MINIO_APP_ACCESS_KEY, MINIO_APP_SECRET_KEY, STORAGE_BUCKET_PREFIX):
  biến môi trường → .env gốc repo → mặc định Docker local. MINIO_ENDPOINT là AWS S3 → dừng (ADR 0010).

    powershell -ExecutionPolicy Bypass -File scripts\seed\upload-assets.ps1
#>
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$ComposeFile = Join-Path $Root "deploy\docker-compose.local.yml"
$ImgDir = Join-Path $Root "web\public\img"
$Utf8 = New-Object System.Text.UTF8Encoding($false)

$DotEnv = @{}
$EnvPath = Join-Path $Root ".env"
if (Test-Path $EnvPath) {
    foreach ($line in [System.IO.File]::ReadAllLines($EnvPath, $Utf8)) {
        if ($line -match '^\s*(export\s+)?([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $v = ($Matches[3] -replace '\s+#.*$', '').Trim()
            if ($v -match '^"(.*)"$' -or $v -match "^'(.*)'$") { $v = $Matches[1] }
            if ($v -ne "") { $DotEnv[$Matches[2]] = $v }
        }
    }
}
function Pick([string]$Name, [string]$Default) {
    $v = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrEmpty($v) -and $DotEnv.ContainsKey($Name)) { $v = $DotEnv[$Name] }
    if ([string]::IsNullOrEmpty($v)) { $v = $Default }
    return $v
}

$Endpoint = Pick "MINIO_ENDPOINT" "http://localhost:9000"
$AccessKey = Pick "MINIO_APP_ACCESS_KEY" "genie-app"
$SecretKey = Pick "MINIO_APP_SECRET_KEY" "changeme"
$Prefix = Pick "STORAGE_BUCKET_PREFIX" ""

if ($Endpoint -match 'amazonaws\.com') {
    Write-Host "DỪNG: MINIO_ENDPOINT=$Endpoint là AWS S3 — ảnh mẫu chỉ đẩy lên Silo máy dev (ADR 0010)."
    exit 3
}
$Target = if ($Endpoint -match '^http://(localhost|127\.0\.0\.1)(:\d+)?/?$') { "http://silo:9000" } else { $Endpoint }
Write-Host "Silo: $Target (máy dev: $Endpoint) · bucket: ${Prefix}product-media, ${Prefix}content-assets"

$Script = "set -e; MC=`$(command -v mcli || command -v mc); " +
    "`$MC alias set seed '$Target' '$AccessKey' '$SecretKey' >/dev/null; " +
    "`$MC mirror --overwrite /assets/products 'seed/${Prefix}product-media/seed/product'; " +
    "`$MC mirror --overwrite /assets/banners 'seed/${Prefix}content-assets/seed/banner'"
& docker compose -f $ComposeFile run --rm --no-deps -v "${ImgDir}:/assets:ro" --entrypoint /bin/sh silo-init -c $Script
if ($LASTEXITCODE -ne 0) { Write-Host "Đẩy ảnh thất bại (docker thoát mã $LASTEXITCODE)."; exit $LASTEXITCODE }
Write-Host ("Xong. Thử: " + $Endpoint.TrimEnd('/') + "/${Prefix}product-media/seed/product/headphones.jpg")

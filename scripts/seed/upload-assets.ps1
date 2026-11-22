<#
  Đẩy ảnh mẫu lên Silo của MÁY DEV — bản Windows PowerShell 5.1 của upload-assets.sh (plan seed 2026-11-16 S04.01, D6).
    web\public\img\products\*  → <tiền tố>product-media/seed/product/
    web\public\img\banners\*   → <tiền tố>content-assets/seed/banner/
    scripts\seed\excel\assets\*.jpg → <tiền tố>product-media/seed/excel/  (ảnh seed từ Excel — plan 2026-11-21 E05.01)
  Cuối cùng đối chiếu mọi khóa 'seed/excel/…' trong R__seed_demo_95_excel_products.sql với Silo — thiếu ảnh → thoát mã 4 (E05.02).
  Đọc cùng biến với backend (MINIO_ENDPOINT, MINIO_APP_ACCESS_KEY, MINIO_APP_SECRET_KEY, STORAGE_BUCKET_PREFIX):
  biến môi trường → .env gốc repo → mặc định Docker local. MINIO_ENDPOINT là AWS S3 → dừng (ADR 0010).

    powershell -ExecutionPolicy Bypass -File scripts\seed\upload-assets.ps1
#>
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$ComposeFile = Join-Path $Root "deploy\docker-compose.local.yml"
$ImgDir = Join-Path $Root "web\public\img"
$ExcelDir = Join-Path $Root "scripts\seed\excel\assets"
$ExcelSql = Join-Path $Root "server-spring\src\main\resources\db\seed\R__seed_demo_95_excel_products.sql"
if (-not (Test-Path $ExcelDir)) { New-Item -ItemType Directory -Path $ExcelDir | Out-Null }
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
    "`$MC mirror --overwrite /assets/banners 'seed/${Prefix}content-assets/seed/banner'; " +
    "`$MC mirror --overwrite --exclude 'manifest.json' /excel 'seed/${Prefix}product-media/seed/excel'"
& docker compose -f $ComposeFile run --rm --no-deps -v "${ImgDir}:/assets:ro" -v "${ExcelDir}:/excel:ro" --entrypoint /bin/sh silo-init -c $Script
if ($LASTEXITCODE -ne 0) { Write-Host "Đẩy ảnh thất bại (docker thoát mã $LASTEXITCODE)."; exit $LASTEXITCODE }

# ---------- E05.02: ảnh nào file SQL 95 cần mà Silo chưa có? (image silo-init không có grep → so ở máy) ----------
if (Test-Path $ExcelSql) {
    $Sql = [System.IO.File]::ReadAllText($ExcelSql, $Utf8)
    $Expected = @([regex]::Matches($Sql, 'seed/excel/[0-9a-f]{8}\.jpg') | ForEach-Object { $_.Value } | Sort-Object -Unique)
    if ($Expected.Count -gt 0) {
        $List = "MC=`$(command -v mcli || command -v mc); " +
            "`$MC alias set seed '$Target' '$AccessKey' '$SecretKey' >/dev/null; " +
            "`$MC ls --recursive 'seed/${Prefix}product-media/seed/excel/'"
        $Present = @(& docker compose -f $ComposeFile run --rm --no-deps --entrypoint /bin/sh silo-init -c $List |
            ForEach-Object { $p = ("$_".Trim() -split '\s+'); "seed/excel/" + $p[-1] })
        $Missing = @($Expected | Where-Object { $Present -notcontains $_ })
        if ($Missing.Count -gt 0) {
            Write-Host "THIẾU ảnh trên Silo (bucket ${Prefix}product-media) mà R__seed_demo_95 cần:"
            $Missing | ForEach-Object { Write-Host "  $_" }
            Write-Host "→ Kiểm folder scripts\seed\excel\assets\ đã pull đủ chưa, và STORAGE_BUCKET_PREFIX / MINIO_ENDPOINT trong .env"
            Write-Host "  phải giống cấu hình backend đang chạy."
            exit 4
        }
        Write-Host "Ảnh seed Excel: đủ $($Expected.Count) ảnh trên Silo."
    }
}
Write-Host ("Xong. Thử: " + $Endpoint.TrimEnd('/') + "/${Prefix}product-media/seed/product/headphones.jpg")

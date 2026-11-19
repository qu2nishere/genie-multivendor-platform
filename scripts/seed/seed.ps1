<#
  Nạp dữ liệu mẫu vào PostgreSQL của MÁY DEV — bản Windows PowerShell 5.1 của seed.sh (plan seed 2026-11-16 S01.04).
  Cùng hành vi: DATABASE_URL > DB_URL + DB_USER + DB_PASSWORD (biến môi trường → .env gốc repo → mặc định Docker local);
  chỉ host local (localhost, 127.0.0.1, ::1, postgres) — production AWS RDS bị từ chối (ADR 0010).

    powershell -ExecutionPolicy Bypass -File scripts\seed\seed.ps1            # psql.exe trên máy
    powershell -ExecutionPolicy Bypass -File scripts\seed\seed.ps1 -Docker    # psql trong container postgres
    powershell -ExecutionPolicy Bypass -File scripts\seed\seed.ps1 -Only 30   # chỉ R__seed_demo_30_*.sql (00 = R__seed_demo.sql)
    powershell -ExecutionPolicy Bypass -File scripts\seed\seed.ps1 -DryRun
#>
param(
    [switch]$Docker,
    [string]$Only = "",
    [switch]$DryRun
)
$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$MigrationDir = Join-Path $Root "server-spring\src\main\resources\db\migration"
$SeedDir = Join-Path $Root "server-spring\src\main\resources\db\seed"
$ComposeFile = Join-Path $Root "deploy\docker-compose.local.yml"
$Utf8 = New-Object System.Text.UTF8Encoding($false)

# ---------- đọc biến: môi trường → .env → mặc định ----------
$DotEnv = @{}
$EnvPath = Join-Path $Root ".env"
if (Test-Path $EnvPath) {
    foreach ($line in [System.IO.File]::ReadAllLines($EnvPath, $Utf8)) {
        if ($line -match '^\s*(export\s+)?([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $v = ($Matches[3] -replace '\s+#.*$', '').Trim()
            if ($v -match '^"(.*)"$' -or $v -match "^'(.*)'$") { $v = $Matches[1] }
            if ($v -ne "") { $DotEnv[$Matches[2]] = $v }   # dòng viết SAU thắng
        }
    }
}
function Pick([string]$Name, [string]$Default) {
    $v = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrEmpty($v) -and $DotEnv.ContainsKey($Name)) { $v = $DotEnv[$Name] }
    if ([string]::IsNullOrEmpty($v)) { $v = $Default }
    return $v
}

$DatabaseUrl = Pick "DATABASE_URL" ""
if ($DatabaseUrl -ne "") {
    $rest = $DatabaseUrl -replace '^[a-z]+://', ''
    $at = $rest.LastIndexOf('@')
    $creds = $rest.Substring(0, $at); $hostPart = $rest.Substring($at + 1)
    $DbUser = ($creds -split ':', 2)[0]
    $DbPassword = if ($creds.Contains(':')) { ($creds -split ':', 2)[1] } else { "" }
} else {
    $DbUrl = Pick "DB_URL" "jdbc:postgresql://localhost:5432/genie"
    $DbUser = Pick "DB_USER" "genie"
    $DbPassword = Pick "DB_PASSWORD" "changeme"
    $hostPart = $DbUrl -replace '^jdbc:postgresql://', ''
}
$hostPort = ($hostPart -split '/', 2)[0]
$dbAndParams = ($hostPart -split '/', 2)[1]
$DbName = ($dbAndParams -split '\?', 2)[0]
$DbParams = if ($dbAndParams.Contains('?')) { ($dbAndParams -split '\?', 2)[1] } else { "" }
if ($hostPort.StartsWith('[')) {
    $DbHost = $hostPort.Substring(0, $hostPort.IndexOf(']') + 1)
    $DbPort = if ($hostPort.Length -gt $DbHost.Length) { $hostPort.Substring($DbHost.Length + 1) } else { "5432" }
} else {
    $DbHost = ($hostPort -split ':', 2)[0]
    $DbPort = if ($hostPort.Contains(':')) { ($hostPort -split ':', 2)[1] } else { "5432" }
}

# ---------- bộ chặn môi trường (N10) ----------
if (@("localhost", "127.0.0.1", "::1", "[::1]", "postgres") -notcontains $DbHost) {
    Write-Host "DỪNG: host CSDL '$DbHost' không phải máy dev (chỉ chấp nhận localhost, 127.0.0.1, ::1, postgres)."
    Write-Host "Dữ liệu mẫu chỉ nạp vào PostgreSQL local; production (AWS RDS) không bao giờ seed — ADR 0010."
    exit 3
}
if ($Docker) { Write-Host "CSDL: container postgres (deploy/docker-compose.local.yml) · db=$DbName · user=$DbUser" }
else { Write-Host "CSDL: ${DbHost}:$DbPort · db=$DbName · user=$DbUser · mật khẩu=****" }

# ---------- danh sách file ----------
$Files = @()
if ($Only -eq "" -or $Only -eq "00") { $Files += (Join-Path $MigrationDir "R__seed_demo.sql") }
$Files += Get-ChildItem -Path $SeedDir -Filter "R__seed_demo_*.sql" | Sort-Object Name |
    Where-Object { $Only -eq "" -or $_.Name.StartsWith("R__seed_demo_${Only}_") } | ForEach-Object { $_.FullName }
if ($Files.Count -eq 0) { Write-Host "Không có file seed nào khớp '-Only $Only'."; exit 2 }
if ($DryRun) {
    Write-Host "Sẽ chạy (theo thứ tự):"
    foreach ($f in $Files) { Write-Host ("  " + $f.Substring($Root.Length + 1)) }
    exit 0
}

# ---------- psql ----------
function Invoke-Psql([string]$Sql, [string[]]$ExtraArgs) {
    # Ghi SQL ra tệp tạm UTF-8 không BOM rồi chuyển vào psql qua stdin (giữ nguyên tiếng Việt)
    $tmp = [System.IO.Path]::GetTempFileName()
    [System.IO.File]::WriteAllText($tmp, $Sql, $Utf8)
    try {
        if ($Docker) {
            $psqlArgs = @("compose", "-f", $ComposeFile, "exec", "-T", "-e", "PGPASSWORD=$DbPassword", "postgres",
                "psql", "-U", $DbUser, "-d", $DbName, "-v", "ON_ERROR_STOP=1", "-q") + $ExtraArgs
            $p = Start-Process -FilePath "docker" -ArgumentList $psqlArgs -RedirectStandardInput $tmp -NoNewWindow -Wait -PassThru
        } else {
            if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
                Write-Host "Không tìm thấy psql.exe — cài PostgreSQL client hoặc dùng -Docker."; exit 4
            }
            $conn = "host=$DbHost port=$DbPort dbname=$DbName user=$DbUser"
            if ($DbParams -ne "") { $conn = "$conn " + ($DbParams -replace '&', ' ') }
            $env:PGPASSWORD = $DbPassword
            $env:PGCLIENTENCODING = "UTF8"
            $psqlArgs = @("`"$conn`"", "-v", "ON_ERROR_STOP=1", "-q") + $ExtraArgs
            $p = Start-Process -FilePath "psql" -ArgumentList $psqlArgs -RedirectStandardInput $tmp -NoNewWindow -Wait -PassThru
        }
        return $p.ExitCode
    } finally {
        Remove-Item $tmp -Force -ErrorAction SilentlyContinue
    }
}

$CountSql = @"
SELECT t.name AS bang, t.n AS so_dong FROM (VALUES
  ('users', (SELECT count(*) FROM users)), ('categories', (SELECT count(*) FROM categories)),
  ('brands', (SELECT count(*) FROM brands)), ('category_attributes', (SELECT count(*) FROM category_attributes)),
  ('vendor_profiles', (SELECT count(*) FROM vendor_profiles)), ('products', (SELECT count(*) FROM products)),
  ('product_variants', (SELECT count(*) FROM product_variants)), ('product_images', (SELECT count(*) FROM product_images)),
  ('vouchers', (SELECT count(*) FROM vouchers)), ('home_banners', (SELECT count(*) FROM home_banners)),
  ('coin_transactions', (SELECT count(*) FROM coin_transactions)), ('user_wishlists', (SELECT count(*) FROM user_wishlists)),
  ('shop_follows', (SELECT count(*) FROM shop_follows)), ('kyc_results', (SELECT count(*) FROM kyc_results))
) AS t(name, n);
"@

# Kiểm schema V14: nếu chưa migrate thì câu DO ném lỗi → psql thoát mã khác 0
$CheckSql = @"
DO `$`$ BEGIN
  IF to_regclass('public.flyway_schema_history') IS NULL OR NOT EXISTS (
       SELECT 1 FROM flyway_schema_history WHERE version IS NOT NULL AND split_part(version, '.', 1)::int >= 14 AND success) THEN
    RAISE EXCEPTION 'CSDL chua co schema V14 - chay backend mot lan de Flyway migrate roi seed lai';
  END IF;
END `$`$;
"@
if ((Invoke-Psql $CheckSql @()) -ne 0) {
    Write-Host "Không kết nối được CSDL hoặc CSDL chưa migrate tới V14 — chạy backend một lần (./mvnw spring-boot:run) rồi seed lại."
    exit 5
}

Write-Host "Trước khi seed:"; [void](Invoke-Psql $CountSql @())
foreach ($f in $Files) {
    Write-Host ("-> " + $f.Substring($Root.Length + 1))
    $sql = [System.IO.File]::ReadAllText($f, $Utf8)
    if ((Split-Path $f -Leaf) -eq "R__seed_demo.sql") { $sql = $sql -replace '\$\{seed_demo\}', 'true' }   # D3
    $code = Invoke-Psql $sql @("--single-transaction")
    if ($code -ne 0) { Write-Host "Lỗi khi chạy $f (psql thoát mã $code)."; exit $code }
}
Write-Host "Sau khi seed:"; [void](Invoke-Psql $CountSql @())
Write-Host "Xong. Ảnh mẫu: scripts\seed\upload-assets.ps1 (đẩy lên Silo)."

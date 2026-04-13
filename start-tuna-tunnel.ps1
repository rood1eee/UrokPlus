# Tuna tunnel -> localhost:8080, subdomain urokplus-u9xw2k7m (reserved).
# Token: tuna-token.local (gitignored) or $env:TUNA_TOKEN.
# Start API first: cd server; node index.js

$Subdomain = "urokplus-u9xw2k7m"

$tokenFile = Join-Path $PSScriptRoot "tuna-token.local"
if (-not $env:TUNA_TOKEN -and (Test-Path $tokenFile)) {
    $env:TUNA_TOKEN = (Get-Content -LiteralPath $tokenFile -Raw).Trim()
}

if (-not $env:TUNA_TOKEN) {
    Write-Error "Set token: create tuna-token.local (one line) or `$env:TUNA_TOKEN = 'tt_...'"
    exit 1
}

$tuna = Get-Command tuna -ErrorAction SilentlyContinue
if (-not $tuna) {
    Write-Error "Command 'tuna' not found. Install CLI from https://tuna.am"
    exit 1
}

Write-Host "API: https://$Subdomain.ru.tuna.am -> localhost:8080 (run node index.js in server/)" -ForegroundColor Cyan
& tuna http 8080 --token=$env:TUNA_TOKEN --subdomain=$Subdomain

param(
    [Parameter(Mandatory = $true)]
    [string]$Url
)

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$localPropsPath = Join-Path $projectRoot "local.properties"

if (-not (Test-Path $localPropsPath)) {
    throw "Файл local.properties не найден: $localPropsPath"
}

$normalizedUrl = $Url.Trim().TrimEnd("/")
$newLine = "api.base.url=$normalizedUrl"

$lines = Get-Content -Path $localPropsPath -Encoding UTF8
$hasKey = $false

for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match '^api\.base\.url=') {
        $lines[$i] = $newLine
        $hasKey = $true
        break
    }
}

if (-not $hasKey) {
    $lines += $newLine
}

Set-Content -Path $localPropsPath -Value $lines -Encoding UTF8
Write-Host "API URL updated: $normalizedUrl"

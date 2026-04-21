param(
    [Parameter(Mandatory = $true)]
    [string]$PublicBaseUrl
)

$ErrorActionPreference = "Stop"

$normalizedBaseUrl = $PublicBaseUrl.Trim().TrimEnd("/")
if (-not $normalizedBaseUrl.StartsWith("https://")) {
    throw "Usa una URL HTTPS publica, por ejemplo: https://crm.example.com"
}

$configDir = Join-Path $env:USERPROFILE ".supportdesk"
$configPath = Join-Path $configDir "supportdesk.properties"

New-Item -ItemType Directory -Force -Path $configDir | Out-Null
Set-Content -Path $configPath -Encoding UTF8 -Value "supportdesk.baseUrl=$normalizedBaseUrl"

Write-Host "Configuracion creada:"
Write-Host $configPath
Write-Host "Backend publico:"
Write-Host $normalizedBaseUrl

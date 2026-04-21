param(
    [Parameter(Mandatory = $true)]
    [string]$SupabaseDatabaseUrl,

    [string]$PublicDomain = "crm.example.com"
)

$ErrorActionPreference = "Stop"

$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$secret = [Convert]::ToBase64String($bytes)

$content = @"
SUPPORTDESK_AUTH_SECRET=$secret
SUPABASE_DATABASE_URL=$SupabaseDatabaseUrl
SUPPORTDESK_SERVER_HOST=127.0.0.1
SUPPORTDESK_SERVER_PORT=8080
SUPPORTDESK_AUTH_ISSUER=$PublicDomain
SUPPORTDESK_AUTH_AUDIENCE=$PublicDomain
"@

Set-Content -Path "supportdesk.properties" -Encoding UTF8 -Value $content

Write-Host "Creado supportdesk.properties para Ktor."
Write-Host "Ahora ejecuta: .\start-ktor-server.ps1"

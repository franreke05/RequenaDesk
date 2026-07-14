param(
    [Parameter(Mandatory = $true)]
    [string]$SupabaseDatabaseUrl,

    [string]$PublicDomain = "crm.example.com",

    [switch]$BootstrapDemoData
)

$ErrorActionPreference = "Stop"

function Read-PlainTextPassword {
    param([string]$Prompt)

    $secureValue = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$secret = [Convert]::ToBase64String($bytes)

$lines = @(
    "SUPPORTDESK_AUTH_SECRET=$secret",
    "SUPABASE_DATABASE_URL=$SupabaseDatabaseUrl",
    "SUPPORTDESK_DB_MAX_POOL_SIZE=5",
    "SUPPORTDESK_SERVER_HOST=127.0.0.1",
    "SUPPORTDESK_SERVER_PORT=8080",
    "SUPPORTDESK_AUTH_ISSUER=$PublicDomain",
    "SUPPORTDESK_AUTH_AUDIENCE=$PublicDomain",
    "SUPPORTDESK_BOOTSTRAP_DEMO_DATA=$($BootstrapDemoData.IsPresent.ToString().ToLowerInvariant())"
)

if ($BootstrapDemoData) {
    $adminPassword = Read-PlainTextPassword "Contrasena inicial de administrador"
    $clientPassword = Read-PlainTextPassword "Contrasena inicial de cliente"
    if ($adminPassword.Length -lt 12 -or $clientPassword.Length -lt 12) {
        throw "Las contrasenas iniciales deben tener al menos 12 caracteres."
    }
    $lines += "SUPPORTDESK_BOOTSTRAP_ADMIN_PASSWORD=$adminPassword"
    $lines += "SUPPORTDESK_BOOTSTRAP_CLIENT_PASSWORD=$clientPassword"
}

$targetPath = Join-Path $PSScriptRoot "supportdesk.properties"
$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($targetPath, (($lines -join [Environment]::NewLine) + [Environment]::NewLine), $utf8WithoutBom)

Write-Host "Creado supportdesk.properties para Ktor."
Write-Host "Ahora ejecuta: .\start-ktor-server.ps1"

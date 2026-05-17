param(
    [switch]$Development
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$gradleWrapper = Join-Path $scriptDir "gradlew.bat"
if (-not (Test-Path $gradleWrapper)) {
    throw "No se encontro gradlew.bat en la raiz del proyecto: $scriptDir"
}

$propertiesPath = Join-Path $scriptDir "supportdesk.properties"
$properties = @{}
if (Test-Path $propertiesPath) {
    Get-Content $propertiesPath | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }
        $parts = $line.Split("=", 2)
        if ($parts.Count -eq 2) {
            $properties[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
}

function Get-EnvValue {
    param([string]$Name)

    $item = Get-Item "Env:$Name" -ErrorAction SilentlyContinue
    if ($null -ne $item -and $item.Value) {
        return $item.Value
    }
    return $properties[$Name]
}

function Ensure-Java {
    $candidates = @()

    if (Get-EnvValue "JAVA_HOME") {
        $candidates += (Get-EnvValue "JAVA_HOME")
    }

    $candidates += @(
        "C:\Program Files\Android\Android Studio\jbr",
        "C:\Program Files\Android\Android Studio\jre",
        "C:\Program Files\JetBrains\Android Studio\jbr",
        "C:\Program Files\JetBrains\Android Studio\jre"
    )

    if ($env:LOCALAPPDATA) {
        $candidates += @(
            (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr"),
            (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jre")
        )
    }

    foreach ($candidate in $candidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -Unique) {
        $javaExe = Join-Path $candidate "bin\java.exe"
        if (Test-Path $javaExe) {
            $env:JAVA_HOME = $candidate
            if (-not ($env:Path -split ";" | Where-Object { $_ -eq (Join-Path $candidate "bin") })) {
                $env:Path = "$(Join-Path $candidate 'bin');$env:Path"
            }
            return
        }
    }

    throw "No se encontro Java. Define JAVA_HOME o instala Android Studio con JBR disponible."
}

function Assert-DatabaseConfig {
    $databaseUrl = Get-EnvValue "SUPABASE_DATABASE_URL"
    if ($databaseUrl) {
        return
    }

    $required = @(
        "SUPABASE_DB_HOST",
        "SUPABASE_DB_USER",
        "SUPABASE_DB_PASSWORD"
    )

    $missing = $required | Where-Object { -not (Get-EnvValue $_) }
    if ($missing.Count -gt 0) {
        throw "Faltan variables de base de datos: $($missing -join ', ')."
    }
}

Ensure-Java

$authSecret = Get-EnvValue "SUPPORTDESK_AUTH_SECRET"
if (-not $authSecret) {
    throw "SUPPORTDESK_AUTH_SECRET es obligatorio."
}

Assert-DatabaseConfig

$serverHost = Get-EnvValue "SUPPORTDESK_SERVER_HOST"
if (-not $serverHost) {
    $serverHost = "0.0.0.0"
}

$serverPort = Get-EnvValue "SUPPORTDESK_SERVER_PORT"
if (-not $serverPort) {
    $serverPort = "8080"
}

$env:SUPPORTDESK_SERVER_HOST = $serverHost
$env:SUPPORTDESK_SERVER_PORT = $serverPort

Write-Host "Iniciando servidor Ktor..."
Write-Host "Host interno: $serverHost"
Write-Host "Puerto interno: $serverPort"
Write-Host "Backend esperado por el proxy: http://$serverHost`:$serverPort"
Write-Host "Android emulator: http://10.0.2.2`:$serverPort"

$gradleArgs = @(":server:run")
if ($Development) {
    $gradleArgs += "-Pdevelopment=true"
}

& $gradleWrapper @gradleArgs

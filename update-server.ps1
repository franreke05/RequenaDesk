<#
.SYNOPSIS
    Actualiza el servidor Ktor de produccion siguiendo docs/setup/update-server-deployment.md.

.DESCRIPTION
    Automatiza los pasos mecanicos de una actualizacion: comprobar que el arbol de trabajo esta
    limpio, traer el codigo nuevo (git pull), detener la version actual de Ktor, arrancar la
    version nueva y esperar a que /health/live y /health/ready respondan. Flyway aplica las
    migraciones pendientes solo, al arrancar Ktor - este script no ejecuta SQL directamente.

    Este script NO hace el backup de la base de datos por ti. Te pedira confirmar que existe
    uno reciente (Supabase Dashboard > Database > Backups) antes de continuar, porque esta
    clase de actualizaciones puede incluir migraciones de esquema irreversibles con Flyway.

.PARAMETER ServiceName
    Si tienes Ktor registrado como servicio de Windows, pasa aqui su nombre; el script usara
    Stop-Service/Start-Service. Si se omite, asume que corre como proceso de consola via
    'gradlew.bat :server:run' (el patron de start-ktor-server.ps1) y lo localiza por linea de
    comandos.

.PARAMETER Branch
    Rama a sincronizar con git pull. Por defecto "main".

.PARAMETER BaseUrl
    URL local del backend para las comprobaciones de salud. Por defecto http://127.0.0.1:8080.

.PARAMETER PublicUrl
    URL publica (a traves de Caddy) para la comprobacion final. Por defecto
    https://crm.franciscorequena.cloud.

.PARAMETER Force
    Omite las confirmaciones interactivas (backup, detener el proceso, rama distinta de la
    esperada). Pensado para uso no interactivo; usalo solo si ya has verificado el backup a mano.

.PARAMETER DryRun
    Muestra cada paso sin ejecutar ningun cambio real (no detiene procesos, no hace git pull,
    no arranca nada).

.PARAMETER SkipGitPull
    No trae codigo nuevo; solo detiene y vuelve a arrancar el proceso actual. Util para un
    reinicio simple o para probar el propio script.

.PARAMETER RunSmokeTest
    Tras confirmar /health/ready, hace login con -AdminEmail y comprueba que
    /admin/work-sessions/active responde (endpoint nuevo de esta actualizacion).

.PARAMETER AdminEmail
    Correo de administrador para el smoke test. Requerido si usas -RunSmokeTest; la contrasena
    se pide de forma segura en el momento, nunca como parametro en texto plano.

.EXAMPLE
    .\update-server.ps1
    Actualizacion interactiva estandar (proceso de consola, con confirmaciones).

.EXAMPLE
    .\update-server.ps1 -ServiceName KtorSupportDesk -RunSmokeTest -AdminEmail admin@crm.example.com
    Actualizacion sobre un servicio de Windows, con verificacion del endpoint nuevo al final.

.EXAMPLE
    .\update-server.ps1 -DryRun
    Revisa qué haria el script sin tocar nada todavia.
#>
param(
    [string]$ServiceName,
    [string]$Branch = "main",
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$PublicUrl = "https://crm.franciscorequena.cloud",
    [switch]$Force,
    [switch]$DryRun,
    [switch]$SkipGitPull,
    [switch]$RunSmokeTest,
    [string]$AdminEmail
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Confirm-OrAbort {
    param([string]$Prompt)
    if ($Force -or $DryRun) {
        return
    }
    $answer = Read-Host "$Prompt (escribe SI para continuar)"
    if ($answer -ne "SI") {
        throw "Cancelado por el usuario."
    }
}

function Invoke-Real {
    param(
        [Parameter(Mandatory = $true)][scriptblock]$Action,
        [Parameter(Mandatory = $true)][string]$Description
    )
    if ($DryRun) {
        Write-Host "  [DRY RUN] $Description" -ForegroundColor DarkYellow
        return
    }
    & $Action
}

# --- Paso 1: recordatorio de backup ------------------------------------------
Write-Step "Paso 1 de 8 - Backup de la base de datos"
Write-Host "Esta actualizacion puede incluir migraciones de esquema (Flyway las aplica sola"
Write-Host "al arrancar Ktor). Confirma que tienes un backup reciente en Supabase Dashboard"
Write-Host "> Database > Backups antes de continuar. Este script NO hace ese backup por ti."
Confirm-OrAbort "He confirmado que existe un backup reciente"

# --- Paso 2: estado de git ----------------------------------------------------
Write-Step "Paso 2 de 8 - Comprobando el arbol de trabajo"
$gitStatus = git status --porcelain
if ($gitStatus) {
    Write-Host $gitStatus
    throw "Hay cambios locales sin commitear en $scriptDir. Resuelvelos (git stash o git commit) antes de actualizar."
}
$currentBranch = git rev-parse --abbrev-ref HEAD
Write-Host "Rama actual: $currentBranch"
if ($currentBranch -ne $Branch) {
    Confirm-OrAbort "La rama actual ('$currentBranch') no es '$Branch'. Continuar de todas formas"
}

# --- Paso 3: git pull ----------------------------------------------------------
if ($SkipGitPull) {
    Write-Step "Paso 3 de 8 - Omitido (-SkipGitPull)"
} else {
    Write-Step "Paso 3 de 8 - Trayendo el codigo nuevo (git pull origin $Branch)"
    Invoke-Real -Description "git fetch origin; git pull origin $Branch" -Action {
        git fetch origin
        git pull origin $Branch
    }
}

# --- Paso 4: detectar y detener el proceso actual ----------------------------
Write-Step "Paso 4 de 8 - Deteniendo la version actual de Ktor"
if ($ServiceName) {
    Write-Host "Modo servicio de Windows: '$ServiceName'"
    Confirm-OrAbort "Se va a detener el servicio '$ServiceName'"
    Invoke-Real -Description "Stop-Service $ServiceName" -Action {
        Stop-Service -Name $ServiceName -ErrorAction SilentlyContinue
    }.GetNewClosure()
} else {
    $ktorProcesses = Get-CimInstance Win32_Process -Filter "CommandLine like '%server:run%'" -ErrorAction SilentlyContinue
    if ($ktorProcesses) {
        Write-Host "Procesos encontrados:"
        $ktorProcesses | ForEach-Object { Write-Host "  PID $($_.ProcessId): $($_.CommandLine)" }
        Confirm-OrAbort "Se van a detener estos procesos"
        Invoke-Real -Description "Stop-Process en los PID anteriores" -Action {
            $ktorProcesses | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
        }.GetNewClosure()
    } else {
        Write-Host "No se encontro ningun proceso 'gradlew ... :server:run' en ejecucion. Se continua."
    }
}

if (-not $DryRun) {
    Write-Host "Esperando a que el puerto 8080 quede libre..."
    $portFreeDeadline = (Get-Date).AddSeconds(30)
    $portBusy = $true
    while ((Get-Date) -lt $portFreeDeadline) {
        $portBusy = [bool](netstat -ano | Select-String ":8080\s+.*LISTENING")
        if (-not $portBusy) {
            break
        }
        Start-Sleep -Seconds 2
    }
    if ($portBusy) {
        Write-Host "Aviso: el puerto 8080 sigue mostrando un listener tras 30s. Continuo de todas formas." -ForegroundColor Yellow
    }
}

# --- Paso 5: arrancar la version nueva ----------------------------------------
Write-Step "Paso 5 de 8 - Arrancando la version nueva"
if ($ServiceName) {
    Invoke-Real -Description "Start-Service $ServiceName" -Action {
        Start-Service -Name $ServiceName
    }.GetNewClosure()
} else {
    $startScript = Join-Path $scriptDir "start-ktor-server.ps1"
    Invoke-Real -Description "Nueva ventana con $startScript" -Action {
        Start-Process powershell -ArgumentList "-NoExit", "-File", $startScript
    }.GetNewClosure()
    if (-not $DryRun) {
        Write-Host "Se abrio una ventana nueva ejecutando start-ktor-server.ps1. Dejala abierta."
    }
}

# --- Paso 6: esperar y comprobar salud ----------------------------------------
Write-Step "Paso 6 de 8 - Esperando a que Ktor responda"
if (-not $DryRun) {
    $healthy = $false
    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-RestMethod -Uri "$BaseUrl/health/live" -TimeoutSec 5 | Out-Null
            Invoke-RestMethod -Uri "$BaseUrl/health/ready" -TimeoutSec 5 | Out-Null
            Write-Host "  live: OK, ready: OK"
            $healthy = $true
            break
        } catch {
            Write-Host "  todavia no responde, reintentando..."
            Start-Sleep -Seconds 5
        }
    }
    if (-not $healthy) {
        throw "Ktor no respondio en $BaseUrl/health/ready tras 120s. Revisa la ventana/log del servidor antes de continuar. Ver la seccion 'Si algo falla' en docs/setup/update-server-deployment.md."
    }
} else {
    Write-Host "  [DRY RUN] esperaria a $BaseUrl/health/live y /health/ready"
}

# --- Paso 7: comprobacion publica ---------------------------------------------
Write-Step "Paso 7 de 8 - Comprobando el dominio publico"
if ($DryRun) {
    Write-Host "  [DRY RUN] GET $PublicUrl/health/ready"
} else {
    try {
        Invoke-RestMethod -Uri "$PublicUrl/health/ready" -TimeoutSec 10 | Out-Null
        Write-Host "  $PublicUrl/health/ready responde correctamente."
    } catch {
        Write-Host "  Aviso: $PublicUrl/health/ready no respondio. Revisa Caddy (no deberia necesitar cambios para esta actualizacion)." -ForegroundColor Yellow
    }
}

# --- Paso 8: smoke test opcional del endpoint nuevo --------------------------
Write-Step "Paso 8 de 8 - Smoke test del endpoint nuevo (/admin/work-sessions/active)"
if (-not $RunSmokeTest) {
    Write-Host "Omitido. Usa -RunSmokeTest -AdminEmail tu@correo para comprobarlo."
} elseif ($DryRun) {
    Write-Host "  [DRY RUN] login + GET /admin/work-sessions/active"
} else {
    if (-not $AdminEmail) {
        throw "-RunSmokeTest requiere -AdminEmail."
    }
    $securePassword = Read-Host "Contrasena de $AdminEmail" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }

    $loginBody = @{ email = $AdminEmail; password = $plainPassword } | ConvertTo-Json
    $loginResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -Body $loginBody -ContentType "application/json"
    $accessToken = $loginResponse.data.accessToken
    if (-not $accessToken) {
        throw "El login no devolvio accessToken. Revisa las credenciales."
    }
    $activeSession = Invoke-RestMethod -Uri "$BaseUrl/admin/work-sessions/active" -Headers @{ Authorization = "Bearer $accessToken" }
    Write-Host "  /admin/work-sessions/active respondio correctamente (status=$($activeSession.status))."
}

Write-Host ""
Write-Host "Actualizacion completada." -ForegroundColor Green
Write-Host "Pendiente manual: probar desde la app de escritorio real (iniciar/pausar/reanudar/detener"
Write-Host "un temporizador y comprobar historial y calendario) - Paso 8 de docs/setup/update-server-deployment.md."

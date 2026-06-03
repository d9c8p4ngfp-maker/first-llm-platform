# Start Docker MySQL for local testing (project on D:)
param([switch]$MysqlOnly)
$ErrorActionPreference = "Stop"
$root = "D:\first"
$tools = "D:\Admin\.tools"
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            Set-Item -Path "env:$($matches[1].Trim())" -Value $matches[2].Trim()
        }
    }
}
Push-Location $root
docker compose up -d mysql
$ok = $false
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 3
    $status = docker inspect --format='{{.State.Health.Status}}' first-mysql 2>$null
    if ($status -eq "healthy") { $ok = $true; break }
}
Pop-Location
if (-not $ok) { Write-Host "MySQL not ready"; exit 1 }
Write-Host "MySQL ready: localhost:3306 / first_gateway / gateway / changeme"
if ($MysqlOnly) { exit 0 }
$env:JAVA_HOME = "$tools\jdk-21"
$env:Path = "$tools\jdk-21\bin;$tools\maven\bin;$env:Path"
Push-Location "$root\first-gateway"
New-Item -ItemType Directory -Path "logs" -Force | Out-Null
$consoleLog = Join-Path (Get-Location) "logs\gateway-console.log"
Write-Host "Starting backend... logs: logs\gateway-dev.log + $consoleLog"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev" 2>&1 | Tee-Object -FilePath $consoleLog

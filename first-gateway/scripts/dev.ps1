$tools = "D:\Admin\.tools"
$env:JAVA_HOME = "$tools\jdk-21"
$env:Path = "$tools\jdk-21\bin;$tools\maven\bin;$env:Path"
Set-Location $PSScriptRoot\..

New-Item -ItemType Directory -Path "logs" -Force | Out-Null
$consoleLog = Join-Path (Get-Location) "logs\gateway-console.log"

Write-Host "Java: $(java -version 2>&1 | Select-Object -First 1)"
Write-Host "Starting first-gateway (profile=dev) on http://localhost:8080 ..."
Write-Host "Console log: $consoleLog"
Write-Host "Access log:  $(Join-Path (Get-Location) 'logs\gateway-dev.log')"
Write-Host "Press Ctrl+C to stop."
mvn spring-boot:run "-Dspring-boot.run.profiles=dev" 2>&1 | Tee-Object -FilePath $consoleLog -Append

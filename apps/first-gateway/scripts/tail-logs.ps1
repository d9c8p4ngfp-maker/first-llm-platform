param([int]$Lines = 100)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$logFile = Join-Path $root "logs\gateway-dev.log"
$consoleLog = Join-Path $root "logs\gateway-console.log"
$logDir = Split-Path $logFile -Parent
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
Write-Host "Backend log console" -ForegroundColor Cyan
Write-Host "Access log:  $logFile" -ForegroundColor DarkGray
Write-Host "Console log: $consoleLog" -ForegroundColor DarkGray
Write-Host "Press Ctrl+C to stop tailing" -ForegroundColor DarkGray
Write-Host ("-" * 72) -ForegroundColor DarkGray
function Show-RecentLogs($Path, $Count, $Label) {
  if (Test-Path $Path) {
    Write-Host "--- $Label ---" -ForegroundColor Yellow
    Get-Content -Path $Path -Tail $Count -Encoding UTF8 -ErrorAction SilentlyContinue
  } else {
    Write-Host "--- $Label not found: $Path ---" -ForegroundColor DarkYellow
  }
}
Show-RecentLogs $logFile $Lines "recent access"
Show-RecentLogs $consoleLog 30 "recent console"
Write-Host "--- live access log ---" -ForegroundColor Green
if (-not (Test-Path $logFile)) {
  while (-not (Test-Path $logFile)) { Start-Sleep -Seconds 1 }
}
$lastSize = (Get-Item $logFile).Length
while ($true) {
  Start-Sleep -Milliseconds 400
  if (-not (Test-Path $logFile)) { continue }
  $currentSize = (Get-Item $logFile).Length
  if ($currentSize -le $lastSize) { continue }
  $stream = [System.IO.File]::Open($logFile, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
  try {
    $stream.Seek($lastSize, [System.IO.SeekOrigin]::Begin) | Out-Null
    $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
    while (-not $reader.EndOfStream) {
      $line = $reader.ReadLine()
      if ($null -ne $line) { Write-Host $line }
    }
    $lastSize = $stream.Length
  } finally { $stream.Close() }
}

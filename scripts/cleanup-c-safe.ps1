# Safe C: cleanup (dev machine). Saves nothing destructive.
# Run: powershell -ExecutionPolicy Bypass -File D:\first\scripts\cleanup-c-safe.ps1

$ErrorActionPreference = "Continue"
$before = (Get-PSDrive C).Free
Write-Host "C: free before: $([math]::Round($before/1GB,2)) GB"

# 1) User temp
foreach ($dir in @($env:TEMP, "$env:LOCALAPPDATA\Temp")) {
  if (Test-Path $dir) {
    Get-ChildItem $dir -Force -EA SilentlyContinue | Remove-Item -Recurse -Force -EA SilentlyContinue
    Write-Host "Cleaned: $dir"
  }
}

# 2) pip cache on C (if any)
$pipC = "$env:LOCALAPPDATA\pip\Cache"
if (Test-Path $pipC) {
  Remove-Item "$pipC\*" -Recurse -Force -EA SilentlyContinue
  Write-Host "Cleaned: $pipC"
}

# 3) npm cache (optional, uncomment if needed)
# npm cache clean --force 2>$null

# 4) Docker prune (only if Docker Desktop is running)
# docker system prune -af 2>$null

$after = (Get-PSDrive C).Free
Write-Host "C: free after:  $([math]::Round($after/1GB,2)) GB"
Write-Host "Freed about $([math]::Round(($after-$before)/1MB,1)) MB"
Write-Host ""
Write-Host "NOT touched: Windows\, Program Files, Documents, .m2 (move to D: manually if needed)"
Write-Host "JDK/Maven/Python live on D:\Admin\.tools (C:\Users\Admin\.tools is a mount point)"
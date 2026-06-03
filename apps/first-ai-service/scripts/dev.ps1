$ErrorActionPreference = "Stop"
$Py = "D:\Admin\.tools\python-3.12\python.exe"
$Root = "D:\first\apps\first-ai-service"
$env:PYTHONPATH = $Root
Set-Location $Root
if (-not (Test-Path $Py)) {
    Write-Error "Run D:\first\scripts\install-python-d.ps1 then setup-ai-python.ps1"
}
Write-Host "Using D: python: $Py"
Write-Host "Starting first-ai-service on http://localhost:8000 ..."
& $Py -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
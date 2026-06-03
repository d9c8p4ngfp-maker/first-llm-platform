$ErrorActionPreference = "Stop"
$Root = "D:\first"
$Py = "D:\Admin\.tools\python-3.12\python.exe"
$PipCache = "D:\Admin\.tools\pip-cache"

if (-not (Test-Path $Py)) {
    Write-Error "Run D:\first\scripts\install-python-d.ps1 first"
}
New-Item -ItemType Directory -Force -Path $PipCache | Out-Null
$env:PIP_CACHE_DIR = $PipCache

Write-Host "Installing first-ai-service deps to D:\Admin\.tools\python-3.12 (pip cache: $PipCache)"
& $Py -m pip install --upgrade pip
& $Py -m pip install -r "$Root\first-ai-service\requirements.txt"
Write-Host "OK: python=$Py"
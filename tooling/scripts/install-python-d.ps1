# Download portable Python 3.12 embed to D:\Admin\.tools (not C:)
$ErrorActionPreference = "Stop"
$dest = "D:\Admin\.tools\python-3.12"
$zip = "D:\Admin\.tools\python-3.12-embed.zip"
$url = "https://www.python.org/ftp/python/3.12.10/python-3.12.10-embed-amd64.zip"

if (Test-Path "$dest\python.exe") {
    Write-Host "Already installed: $dest\python.exe"
    exit 0
}

New-Item -ItemType Directory -Force -Path "D:\Admin\.tools" | Out-Null
Write-Host "Downloading Python embed to D: ..."
Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
if (Test-Path $dest) { Remove-Item -Recurse -Force $dest }
Expand-Archive -Path $zip -DestinationPath $dest -Force
Remove-Item $zip -Force

# Enable pip in embed: uncomment import site + install pip
$pth = Get-ChildItem "$dest\python*._pth" | Select-Object -First 1
if ($pth) {
    (Get-Content $pth.FullName) -replace '#import site', 'import site' | Set-Content $pth.FullName -Encoding ASCII
}
$getPip = "D:\Admin\.tools\get-pip.py"
Invoke-WebRequest -Uri "https://bootstrap.pypa.io/get-pip.py" -OutFile $getPip -UseBasicParsing
& "$dest\python.exe" $getPip --no-warn-script-location
Remove-Item $getPip -Force
Write-Host "OK: $dest\python.exe"
# Initialize monorepo and prepare first commit (run after Git is installed)
# Install: https://git-scm.com/download/win

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
Set-Location $root

function Find-Git {
    $candidates = @(
        "git",
        "C:\Program Files\Git\cmd\git.exe",
        "C:\Program Files\Git\bin\git.exe"
    )
    foreach ($c in $candidates) {
        if ($c -eq "git") {
            $cmd = Get-Command git -ErrorAction SilentlyContinue
            if ($cmd) { return $cmd.Source }
        } elseif (Test-Path $c) { return $c }
    }
    return $null
}

$git = Find-Git
if (-not $git) {
    Write-Host "Git not found. Install from https://git-scm.com/download/win then re-run." -ForegroundColor Red
    exit 1
}
Write-Host "Using Git: $git"

if (Test-Path ".env") {
    Write-Host "OK: .env exists locally and is gitignored (will NOT be committed)." -ForegroundColor Green
}

if (-not (Test-Path ".git")) {
    & $git init
    Write-Host "Initialized git repository."
} else {
    Write-Host "Git repo already exists."
}

& $git add .
$status = & $git status --short
if ($status -match "\.env") {
    Write-Host "ERROR: .env is staged! Check .gitignore." -ForegroundColor Red
    & $git reset HEAD .env 2>$null
    exit 1
}

$count = (& $git status --short | Measure-Object -Line).Lines
Write-Host "Staged $count paths (approx)."
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Create empty repo on GitHub (e.g. first-llm-platform)"
Write-Host "  2. Run:"
Write-Host "     git commit --trailer "Co-authored-by: Cursor <cursoragent@cursor.com>" -m `"Initial commit: gateway, web, ai-service monorepo`""
Write-Host "     git branch -M main"
Write-Host "     git remote add origin https://github.com/<USER>/<REPO>.git"
Write-Host "     git push -u origin main"
Write-Host ""
Write-Host "Or: gh auth login && gh repo create first-llm-platform --private --source=. --remote=origin --push"
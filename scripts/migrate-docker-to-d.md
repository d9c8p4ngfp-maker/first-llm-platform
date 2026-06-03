# Docker Desktop migrate to D: (Windows WSL2)

Target folder (recommended): D:\Docker\DockerDesktopWSL

Current data on C: (your machine):
  C:\Users\Admin\AppData\Local\Docker\wsl\disk\docker_data.vhdx  (~2.7 GB)
  C:\Users\Admin\AppData\Local\Docker\wsl\main\ext4.vhdx         (~104 MB)

Your first-gateway MySQL volume is ALREADY on D: (D:\first\data\mysql) - only Docker Desktop engine data moves.

---

## Method A - UI (try first, Docker 4.32+)

1. Right-click Docker whale -> Quit Docker Desktop (fully exit).
2. Open Docker Desktop again only after step 3 if migration fails; for migration keep it quit.
3. Settings (gear) -> Resources -> Advanced
4. Disk image location -> Browse -> choose: D:\Docker\DockerDesktopWSL
5. Apply and restart
6. Wait until migration finishes (several minutes for ~3 GB).

If error "cannot move the file to a different disk drive":
  - Update Docker Desktop first (you are on 4.75, OK)
  - Use Method B below

After success, verify:
  dir D:\Docker\DockerDesktopWSL\disk\docker_data.vhdx
  docker run hello-world

---

## Method B - Manual WSL export/import (keeps images/containers)

Run in PowerShell **as Administrator**:

```powershell
# 1) Stop everything
wsl --shutdown
# Quit Docker Desktop from tray

# 2) Prepare D:
New-Item -ItemType Directory -Force -Path D:\Docker\backup, D:\Docker\DockerDesktopWSL | Out-Null

# 3) Export (adjust distro name if wsl -l -v shows different)
wsl --export docker-desktop-data D:\Docker\backup\docker-desktop-data.tar
# If docker-desktop-data does not exist, only docker-desktop is used in newer versions:
# Copy vhdx manually instead (Method C)

# 4) Unregister old distro on C:
wsl --unregister docker-desktop-data

# 5) Import to D:
wsl --import docker-desktop-data D:\Docker\DockerDesktopWSL D:\Docker\backup\docker-desktop-data.tar --version 2

# 6) Start Docker Desktop
```

Note: Newer Docker may use single distro `docker-desktop` only. Check: wsl -l -v

---

## Method C - Copy vhdx + settings (when only docker-desktop distro)

1. Quit Docker Desktop, wsl --shutdown
2. xcopy /E /I:
   C:\Users\Admin\AppData\Local\Docker\wsl
   D:\Docker\DockerDesktopWSL\wsl
3. In Docker Desktop Settings -> Resources -> Advanced set disk path to D:\Docker\DockerDesktopWSL
4. If old C: files remain after success, delete ONLY after confirming docker works:
   Remove-Item C:\Users\Admin\AppData\Local\Docker\wsl -Recurse -Force

---

## After migration - free C: space

- docker_data.vhdx should live under D:\Docker\...
- Optional: docker system prune (removes unused images, not migration)

## Do NOT delete

- D:\first\data\mysql  (your project database)
- D:\Admin\.tools

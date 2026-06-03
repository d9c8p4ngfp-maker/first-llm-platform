# Helper: write Admin JWT sources as UTF-8 (no BOM)
$utf8 = New-Object System.Text.UTF8Encoding $false
$root = Split-Path $PSScriptRoot -Parent

function Write-Utf8File($relativePath, $content) {
    $path = Join-Path $root $relativePath
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, $content, $utf8)
}

Write-Utf8File "src\main\java\com\first\gateway\repository\UserTenantRelRepository.java" @'
package com.first.gateway.repository;

import com.first.gateway.domain.entity.UserTenantRel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTenantRelRepository extends JpaRepository<UserTenantRel, Long> {

    Optional<UserTenantRel> findFirstByUserIdOrderByJoinedAtAsc(Long userId);

    List<UserTenantRel> findByUserId(Long userId);
}
'@

Write-Host "Done"

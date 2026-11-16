@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul 2>nul

set "REPO_URL=https://github.com/ANTONIOALGMAR/psicology.git"
set "ZIP_URL=https://github.com/ANTONIOALGMAR/psicology/archive/refs/heads/main.zip"
if "%PSICARE_DIR%"=="" (set "INSTALL_DIR=%USERPROFILE%\PsiCare") else (set "INSTALL_DIR=%PSICARE_DIR%")

echo ==========================================
echo   PsiCare - Instalacao e execucao automatica
echo ==========================================
echo.

REM 1) Verificar Docker
where docker >nul 2>nul
if %errorlevel% neq 0 (
  echo [ERRO] Docker nao encontrado.
  echo Instale o Docker Desktop: https://www.docker.com/products/docker-desktop/
  echo Depois de instalar, abra o Docker Desktop e execute este arquivo novamente.
  pause
  exit /b 1
)

docker info >nul 2>nul
if %errorlevel% neq 0 (
  echo [ERRO] O Docker esta instalado, mas nao esta em execucao.
  echo Abra o Docker Desktop e tente novamente.
  pause
  exit /b 1
)

docker compose version >nul 2>nul
if %errorlevel% equ 0 (
  set "COMPOSE=docker compose"
) else (
  where docker-compose >nul 2>nul
  if %errorlevel% equ 0 (
    set "COMPOSE=docker-compose"
  ) else (
    echo [ERRO] Docker Compose nao encontrado. Atualize o Docker Desktop.
    pause
    exit /b 1
  )
)

echo [OK] Docker encontrado.

REM 2) Baixar/atualizar o sistema
if exist "%INSTALL_DIR%\.git" (
  echo [..] Atualizando o sistema em %INSTALL_DIR% ...
  git -C "%INSTALL_DIR%" pull --ff-only >nul 2>nul || echo [AVISO] Nao foi possivel atualizar automaticamente.
) else if exist "%INSTALL_DIR%\docker-compose.yml" (
  echo [OK] Sistema ja encontrado em %INSTALL_DIR%
) else (
  echo [..] Baixando o sistema para %INSTALL_DIR% ...
  where git >nul 2>nul
  if %errorlevel% equ 0 (
    git clone "%REPO_URL%" "%INSTALL_DIR%"
    if !errorlevel! neq 0 goto :downloadzip
  ) else (
    goto :downloadzip
  )
  goto :afterdownload
  :downloadzip
  set "TMPZIP=%TEMP%\psicare-main.zip"
  set "TMPDIR=%TEMP%\psicare-extract"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Invoke-WebRequest -Uri '%ZIP_URL%' -OutFile '%TMPZIP%' -UseBasicParsing; if (Test-Path '%TMPDIR%') { Remove-Item -Recurse -Force '%TMPDIR%' }; Expand-Archive -Path '%TMPZIP%' -DestinationPath '%TMPDIR%' -Force; Move-Item -Path '%TMPDIR%\psicology-main' -Destination '%INSTALL_DIR%'; Remove-Item -Force '%TMPZIP%'; Remove-Item -Recurse -Force '%TMPDIR%' } catch { exit 1 }"
  if %errorlevel% neq 0 (
    echo [ERRO] Nao foi possivel baixar automaticamente.
    echo Baixe manualmente em: https://github.com/ANTONIOALGMAR/psicology ^(Code ^> Download ZIP^)
    pause
    exit /b 1
  )
)
:afterdownload

REM 3) Iniciar
cd /d "%INSTALL_DIR%"
echo [..] Construindo e iniciando o PsiCare (pode levar alguns minutos na primeira vez)...
%COMPOSE% up --build -d
if %errorlevel% neq 0 (
  echo [ERRO] Ocorreu um erro ao iniciar. Verifique se o Docker Desktop esta aberto.
  pause
  exit /b 1
)

echo.
echo ==========================================
echo   Sistema iniciado com sucesso!
echo   Abra no navegador: http://localhost:3000
echo ==========================================
echo Pasta do sistema: %INSTALL_DIR%
start http://localhost:3000
pause

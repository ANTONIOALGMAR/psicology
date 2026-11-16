@echo off
cd /d "%~dp0"
echo ==========================================
echo   PsiCare - Iniciando o sistema
echo ==========================================
where docker >nul 2>nul
if %errorlevel% neq 0 (
  echo ERRO: Docker nao encontrado. Instale o Docker Desktop: https://www.docker.com/products/docker-desktop/
  pause
  exit /b 1
)
echo Construindo e iniciando (pode levar alguns minutos na primeira vez)...
docker compose up --build -d
if %errorlevel% neq 0 (
  echo Ocorreu um erro ao iniciar. Verifique se o Docker Desktop esta aberto.
  pause
  exit /b 1
)
echo.
echo Sistema iniciado!
echo Abra no navegador: http://localhost:3000
start http://localhost:3000
pause

@echo off
cd /d "%~dp0"
echo Parando o PsiCare...
docker compose down
echo Sistema parado.
pause

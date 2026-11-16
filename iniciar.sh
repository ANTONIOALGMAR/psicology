#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
echo "=========================================="
echo "  PsiCare - Iniciando o sistema"
echo "=========================================="
if ! command -v docker >/dev/null 2>&1; then
  echo "ERRO: Docker nao encontrado. Instale o Docker Desktop: https://www.docker.com/products/docker-desktop/"
  exit 1
fi
echo "Construindo e iniciando (pode levar alguns minutos na primeira vez)..."
docker compose up --build -d
echo ""
echo "Sistema iniciado!"
echo "Abra no navegador: http://localhost:3000"
echo "Para parar: execute ./parar.sh"

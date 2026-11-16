#!/usr/bin/env bash
set -e

REPO_URL="https://github.com/ANTONIOALGMAR/psicology.git"
ZIP_URL="https://github.com/ANTONIOALGMAR/psicology/archive/refs/heads/main.zip"
INSTALL_DIR="${PSICARE_DIR:-$HOME/PsiCare}"

echo "=========================================="
echo "  PsiCare - Instalação e execução automática"
echo "=========================================="
echo ""

# 1) Verificar Docker
if ! command -v docker >/dev/null 2>&1; then
  echo "[ERRO] Docker não encontrado."
  echo "Instale o Docker antes de continuar:"
  echo "  - Windows/Mac: https://www.docker.com/products/docker-desktop/"
  echo "  - Ubuntu/Debian: https://docs.docker.com/engine/install/"
  echo "Depois de instalar, abra o Docker Desktop (se aplicável) e execute este script novamente."
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "[ERRO] O Docker está instalado, mas não está em execução."
  echo "Abra o Docker Desktop e tente novamente."
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE="docker-compose"
else
  echo "[ERRO] Docker Compose não encontrado. Atualize o Docker Desktop ou instale o Docker Compose."
  exit 1
fi

echo "[OK] Docker encontrado."

# 2) Baixar/atualizar o sistema
mkdir -p "$(dirname "$INSTALL_DIR")"
if [ -d "$INSTALL_DIR/.git" ]; then
  echo "[..] Atualizando o sistema em $INSTALL_DIR ..."
  git -C "$INSTALL_DIR" pull --ff-only || echo "[AVISO] Não foi possível atualizar automaticamente (continuando com a versão local)."
elif [ -f "$INSTALL_DIR/docker-compose.yml" ]; then
  echo "[OK] Sistema já encontrado em $INSTALL_DIR"
else
  echo "[..] Baixando o sistema para $INSTALL_DIR ..."
  if command -v git >/dev/null 2>&1; then
    git clone "$REPO_URL" "$INSTALL_DIR"
  elif command -v curl >/dev/null 2>&1 && command -v unzip >/dev/null 2>&1; then
    TMPZIP="$(mktemp /tmp/psicare-XXXXXX.zip)"
    curl -L "$ZIP_URL" -o "$TMPZIP"
    TMPDIR="$(mktemp -d /tmp/psicare-XXXXXX)"
    unzip -q "$TMPZIP" -d "$TMPDIR"
    mv "$TMPDIR"/psicology-main "$INSTALL_DIR"
    rm -rf "$TMPZIP" "$TMPDIR"
  else
    echo "[ERRO] Não foi possível baixar automaticamente (é necessário ter 'git' ou 'curl'+'unzip')."
    echo "Baixe manualmente em: https://github.com/ANTONIOALGMAR/psicology (Code > Download ZIP)"
    exit 1
  fi
fi

# 3) Iniciar
cd "$INSTALL_DIR"
echo "[..] Construindo e iniciando o PsiCare (pode levar alguns minutos na primeira vez)..."
$COMPOSE up --build -d

echo ""
echo "=========================================="
echo "  Sistema iniciado com sucesso!"
echo "  Abra no navegador: http://localhost:3000"
echo "=========================================="
echo "Pasta do sistema: $INSTALL_DIR"
echo "Para parar: cd \"$INSTALL_DIR\" && $COMPOSE down"

# Tentar abrir navegador
if command -v xdg-open >/dev/null 2>&1; then xdg-open "http://localhost:3000" >/dev/null 2>&1 || true; fi
if command -v open >/dev/null 2>&1; then open "http://localhost:3000" >/dev/null 2>&1 || true; fi

#!/usr/bin/env bash
set -e

REPO_URL="https://github.com/ANTONIOALGMAR/psicology.git"
ZIP_URL="https://github.com/ANTONIOALGMAR/psicology/archive/refs/heads/main.zip"
INSTALL_DIR="${PSICARE_DIR:-$HOME/PsiCare}"

echo "=========================================="
echo "  PsiCare - Instalação completa e execução"
echo "=========================================="
echo ""

have_cmd() { command -v "$1" >/dev/null 2>&1; }

install_docker() {
  echo "[..] Tentando instalar o Docker automaticamente..."
  case "$(uname -s)" in
    Darwin)
      if have_cmd brew; then
        echo "[..] Instalando Docker Desktop via Homebrew..."
        brew install --cask docker || true
      else
        echo "[..] Homebrew não encontrado. Baixando página do Docker Desktop para Mac..."
        open "https://www.docker.com/products/docker-desktop/" >/dev/null 2>&1 || true
        echo "Instale o Docker Desktop manualmente, abra-o e execute este script novamente."
        exit 1
      fi
      ;;
    Linux)
      if have_cmd curl; then
        echo "[..] Instalando Docker Engine (script oficial). Pode solicitar senha de administrador (sudo)."
        curl -fsSL https://get.docker.com -o /tmp/get-docker.sh
        if have_cmd sudo; then
          sudo sh /tmp/get-docker.sh
          sudo systemctl enable docker >/dev/null 2>&1 || true
          sudo systemctl start docker >/dev/null 2>&1 || true
          sudo usermod -aG docker "$USER" >/dev/null 2>&1 || true
        else
          sh /tmp/get-docker.sh
          systemctl enable docker >/dev/null 2>&1 || true
          systemctl start docker >/dev/null 2>&1 || true
        fi
        rm -f /tmp/get-docker.sh
        echo "[AVISO] Se o Docker foi instalado agora, pode ser necessário sair e entrar novamente na sessão (ou reiniciar) para usar sem sudo."
      else
        echo "[ERRO] 'curl' não encontrado. Instale o Docker manualmente: https://docs.docker.com/engine/install/"
        exit 1
      fi
      ;;
    *)
      echo "[ERRO] Sistema não suportado para instalação automática. Instale o Docker: https://www.docker.com/products/docker-desktop/"
      exit 1
      ;;
  esac
}

start_docker_if_possible() {
  case "$(uname -s)" in
    Darwin)
      open -a Docker >/dev/null 2>&1 || true
      ;;
    Linux)
      if have_cmd systemctl; then
        sudo systemctl start docker >/dev/null 2>&1 || systemctl start docker >/dev/null 2>&1 || true
      fi
      ;;
  esac
}

# 1) Verificar/instalar Docker
if ! have_cmd docker; then
  install_docker
fi

if ! have_cmd docker; then
  echo "[ERRO] Docker ainda não está disponível após a tentativa de instalação."
  echo "Conclua a instalação do Docker, abra-o e execute este script novamente."
  exit 1
fi

# Tentar iniciar Docker se não estiver rodando
if ! docker info >/dev/null 2>&1; then
  echo "[..] Docker não está em execução. Tentando iniciar..."
  start_docker_if_possible
  echo "[..] Aguardando o Docker iniciar (até ~90s)..."
  for i in $(seq 1 30); do
    if docker info >/dev/null 2>&1; then break; fi
    sleep 3
  done
fi

if ! docker info >/dev/null 2>&1; then
  echo "[ERRO] O Docker está instalado, mas não está em execução/pronto."
  echo "Abra o Docker Desktop (Windows/Mac) ou inicie o serviço Docker (Linux) e execute este script novamente."
  echo "Observação: em alguns casos é necessário reiniciar o computador após instalar o Docker."
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif have_cmd docker-compose; then
  COMPOSE="docker-compose"
else
  echo "[ERRO] Docker Compose não encontrado. Atualize o Docker Desktop/instalação do Docker."
  exit 1
fi

echo "[OK] Docker pronto."

# 2) Baixar/atualizar o sistema
mkdir -p "$(dirname "$INSTALL_DIR")"
if [ -d "$INSTALL_DIR/.git" ]; then
  echo "[..] Atualizando o sistema em $INSTALL_DIR ..."
  git -C "$INSTALL_DIR" pull --ff-only || echo "[AVISO] Não foi possível atualizar automaticamente (continuando com a versão local)."
elif [ -f "$INSTALL_DIR/docker-compose.yml" ]; then
  echo "[OK] Sistema já encontrado em $INSTALL_DIR"
else
  echo "[..] Baixando o sistema para $INSTALL_DIR ..."
  if have_cmd git; then
    git clone "$REPO_URL" "$INSTALL_DIR"
  elif have_cmd curl && have_cmd unzip; then
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
if [ "$(uname -s)" = "Darwin" ]; then open "http://localhost:3000" >/dev/null 2>&1 || true; fi
if have_cmd xdg-open; then xdg-open "http://localhost:3000" >/dev/null 2>&1 || true; fi

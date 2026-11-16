#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
echo "Parando o PsiCare..."
docker compose down
echo "Sistema parado."

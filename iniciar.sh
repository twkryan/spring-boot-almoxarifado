#!/usr/bin/env bash
# Almox TI - inicia o sistema a partir do almox-ti.jar (se existir nesta pasta)
# ou do codigo-fonte com o Maven Wrapper.
set -u

cd "$(dirname "$0")"

echo "=========================================================="
echo "  Almox TI - Almoxarifado de TI e Manutenção"
echo "=========================================================="
echo

# ---- 1. O Java esta instalado? ----
if ! command -v java >/dev/null 2>&1; then
    cat <<'FIM'
[ERRO] O Java não foi encontrado neste computador.

Instale o Java 21 (Temurin) e rode este script novamente:
  https://adoptium.net/temurin/releases/?version=21

Linux (Ubuntu/Debian): sudo apt install openjdk-21-jdk
macOS (Homebrew):      brew install --cask temurin@21
FIM
    exit 1
fi

# ---- 2. A versão do Java é 21 ou mais nova? ----
VERSAO="$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
if [ -z "$VERSAO" ]; then
    echo "[ERRO] Não foi possível identificar a versão do Java instalado."
    echo "        Rode 'java -version' e confira se ele funciona."
    exit 1
fi

MAIOR="${VERSAO%%.*}"
case "$MAIOR" in
    1) MAIOR="$(printf '%s' "$VERSAO" | cut -d. -f2)" ;;  # formato antigo 1.8.0
esac
case "$MAIOR" in
    ''|*[!0-9]*)
        echo "[ERRO] Versão do Java não reconhecida: $VERSAO"
        echo "        Instale o Java 21 (Temurin): https://adoptium.net/temurin/releases/?version=21"
        exit 1
        ;;
esac

if [ "$MAIOR" -lt 21 ]; then
    echo "[ERRO] O Java instalado ($VERSAO) é antigo demais. O Almox TI precisa do Java 21 ou mais novo."
    echo "        Baixe em: https://adoptium.net/temurin/releases/?version=21"
    exit 1
fi

echo "Verificando o Java... encontrado na versão $VERSAO."
echo "Tudo pronto. O sistema vai abrir em: http://localhost:8080"
echo "(Para encerrar, pressione Ctrl+C.)"
echo

# ---- 3. Jar pronto na pasta? Usa ele. Senão, roda pelo código-fonte. ----
if [ -f almox-ti.jar ]; then
    echo "Usando o arquivo almox-ti.jar desta pasta..."
    echo
    exec java -jar almox-ti.jar
fi

if [ ! -f mvnw ]; then
    cat <<'FIM'
[ERRO] Nenhum almox-ti.jar encontrado e o projeto Maven não está aqui.

Baixe o almox-ti.jar em:
  https://github.com/twkryan/spring-boot-almoxarifado/releases/latest
e coloque-o nesta pasta.
FIM
    exit 1
fi

if [ ! -x mvnw ]; then
    chmod +x mvnw 2>/dev/null || true
fi

echo "Compilando e iniciando pelo código-fonte (a primeira vez demora um pouco)..."
echo
exec ./mvnw spring-boot:run

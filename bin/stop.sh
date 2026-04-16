#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

APP_NAME="facturacion"
PID_FILE="${PROJECT_DIR}/logs/app.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "[ERROR] Archivo PID no encontrado en $PID_FILE."
    echo "[ERROR] ¿Está la aplicación corriendo?"
    exit 1
fi

PID=$(cat "$PID_FILE")

if kill -0 "$PID" 2>/dev/null; then
    echo "[INFO] Deteniendo $APP_NAME (PID $PID)..."
    kill "$PID"

    # Esperar hasta 10 segundos a que el proceso termine limpiamente
    TIMEOUT=10
    while kill -0 "$PID" 2>/dev/null && [ $TIMEOUT -gt 0 ]; do
        sleep 1
        TIMEOUT=$((TIMEOUT - 1))
    done

    if kill -0 "$PID" 2>/dev/null; then
        echo "[WARN] El proceso no terminó. Forzando cierre (SIGKILL)..."
        kill -9 "$PID"
    fi

    rm -f "$PID_FILE"
    echo "[INFO] Aplicación detenida correctamente."
else
    echo "[WARN] No se encontró proceso con PID $PID. Limpiando archivo PID."
    rm -f "$PID_FILE"
    exit 1
fi

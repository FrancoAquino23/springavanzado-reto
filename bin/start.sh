#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

APP_NAME="facturacion"
JAR_PATH="${PROJECT_DIR}/target/${APP_NAME}-0.0.1-SNAPSHOT.jar"
LOG_DIR="${PROJECT_DIR}/logs"
LOG_FILE="${LOG_DIR}/app.log"
PID_FILE="${LOG_DIR}/app.pid"

mkdir -p "$LOG_DIR"

# Evitar arranque doble
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "[ERROR] La aplicación ya está en ejecución con PID $PID."
        exit 1
    else
        echo "[WARN] Archivo PID obsoleto encontrado. Limpiando..."
        rm -f "$PID_FILE"
    fi
fi

# Construir si el JAR no existe
if [ ! -f "$JAR_PATH" ]; then
    echo "[INFO] JAR no encontrado. Construyendo con Maven..."
    cd "$PROJECT_DIR" && ./mvnw clean package -DskipTests
fi

echo "[INFO] Iniciando $APP_NAME..."
echo "[INFO] Log: $LOG_FILE"

nohup java -jar "$JAR_PATH" >> "$LOG_FILE" 2>&1 &

APP_PID=$!
echo "$APP_PID" > "$PID_FILE"

echo "[INFO] Aplicación iniciada con PID $APP_PID."
echo "[INFO] Usa 'bin/stop.sh' para detenerla."

#!/bin/bash
set -e

APP_NAME="facturacion"
JAR_PATH="target/${APP_NAME}-0.0.1-SNAPSHOT.jar"
LOG_FILE="logs/${APP_NAME}.log"
PID_FILE="logs/${APP_NAME}.pid"

mkdir -p logs

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if kill -0 "$PID" 2>/dev/null; then
    echo "La aplicación ya está en ejecución con PID $PID."
    exit 1
  fi
fi

if [ ! -f "$JAR_PATH" ]; then
  echo "JAR no encontrado en $JAR_PATH. Construyendo el proyecto..."
  ./mvnw clean package -DskipTests
fi

echo "Iniciando $APP_NAME..."
nohup java -jar "$JAR_PATH" \
  --spring.profiles.active=default \
  >> "$LOG_FILE" 2>&1 &

echo $! > "$PID_FILE"
echo "Aplicación iniciada con PID $(cat $PID_FILE). Log: $LOG_FILE"

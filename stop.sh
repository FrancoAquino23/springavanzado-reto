#!/bin/bash

APP_NAME="facturacion"
PID_FILE="logs/${APP_NAME}.pid"

if [ ! -f "$PID_FILE" ]; then
  echo "Archivo PID no encontrado. ¿Está la aplicación corriendo?"
  exit 1
fi

PID=$(cat "$PID_FILE")

if kill -0 "$PID" 2>/dev/null; then
  echo "Deteniendo $APP_NAME (PID $PID)..."
  kill "$PID"
  rm -f "$PID_FILE"
  echo "Aplicación detenida."
else
  echo "No se encontró proceso con PID $PID. Limpiando archivo PID."
  rm -f "$PID_FILE"
  exit 1
fi

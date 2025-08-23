#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$SCRIPT_DIR/server.pid"

if [ ! -f "$PID_FILE" ]; then
  echo "No PID file found. Is the application running?"
  exit 1
fi

PID=$(cat "$PID_FILE")
if ps -p "$PID" > /dev/null 2>&1; then
  echo "Stopping application with PID $PID ..."
  kill "$PID"
  # Wait for process to terminate
  TIMEOUT=30
  while ps -p "$PID" > /dev/null 2>&1 && [ $TIMEOUT -gt 0 ]; do
    sleep 1
    TIMEOUT=$((TIMEOUT-1))
  done
  if ps -p "$PID" > /dev/null 2>&1; then
    echo "Process $PID did not terminate, trying force kill."
    kill -9 "$PID"
  fi
  echo "Application stopped."
else
  echo "No running process found with PID $PID."
fi

rm -f "$PID_FILE"

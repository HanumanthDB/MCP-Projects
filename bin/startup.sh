#!/bin/bash

# Directory where the script resides
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="$PROJECT_ROOT/swagger-mcp-server/target/swagger-mcp-server-1.0.0-SNAPSHOT.jar"
PID_FILE="$SCRIPT_DIR/server.pid"
LOG_FILE="$PROJECT_ROOT/log/server.log"

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if ps -p "$PID" > /dev/null 2>&1; then
    echo "Application already running with PID $PID"
    exit 1
  else
    echo "Removing stale pid file"
    rm "$PID_FILE"
  fi
fi

echo "Starting application..."
nohup java -jar "$JAR" > "$LOG_FILE" 2>&1 &
NEW_PID=$!
echo $NEW_PID > "$PID_FILE"
echo "Application started with PID $NEW_PID. Logs: $LOG_FILE"

#!/usr/bin/env bash
set -euo pipefail

# run.sh — compile and run the Java CLI and serve the web UI
# - Compiles ChatBot.java
# - Starts a simple static server on port 8000
# - Runs the Java CLI in the foreground
# - When the CLI exits, the static server is stopped

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$HERE"

echo "Compiling ChatBot.java..."
javac ChatBot.java

PORT=8000
LOG="${HERE}/static-server.log"

echo "Starting static server on http://localhost:${PORT} (logs -> ${LOG})"
# Start the Python HTTP server in background
python3 -m http.server ${PORT} >/dev/null 2>"${LOG}" &
SERVER_PID=$!

# Give the server a moment to start
sleep 0.3

echo "Static server PID: ${SERVER_PID}"

# Run the Java CLI (foreground so user can interact)
echo "Launching ChatBot CLI. Press Ctrl+C to exit the CLI when finished."
java ChatBot || true

# After CLI exits, stop the static server
if ps -p ${SERVER_PID} > /dev/null 2>&1; then
  echo "Stopping static server (PID ${SERVER_PID})..."
  kill ${SERVER_PID} 2>/dev/null || true
  wait ${SERVER_PID} 2>/dev/null || true
fi

echo "All done."

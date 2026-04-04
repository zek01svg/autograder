#!/bin/bash
# Start the AutoGrader Web UI.
# Usage: ./scripts/web.sh [--port 8080]
java -cp out grader.web.WebMain "$@"

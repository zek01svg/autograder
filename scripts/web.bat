@echo off
REM Start the AutoGrader Web UI.
REM Usage: scripts\web.bat [--port 8080]
java -cp out grader.web.WebMain %*

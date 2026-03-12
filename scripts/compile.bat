@echo off
REM Compile all Java sources in src\ into out\ directory.
echo Compiling...
if not exist out mkdir out
javac -d out -sourcepath src src\grader\Main.java
if %ERRORLEVEL% neq 0 (
    echo Compilation FAILED.
    exit /b 1
)
echo Compilation successful. Output in out\

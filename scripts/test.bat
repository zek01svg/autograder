@echo off
REM Test runner for the AutoGrader project

echo === Compiling Project and Tests ===
if not exist out mkdir out
javac -d out -sourcepath src;tests src\grader\Main.java tests\grader\test\GraderTest.java tests\grader\test\IntegrationTest.java
if %ERRORLEVEL% neq 0 (
    echo Compilation FAILED.
    exit /b 1
)

echo.
echo === Running Unit Tests: GraderTest ===
java -cp out grader.test.GraderTest
if %ERRORLEVEL% neq 0 exit /b 1

echo.
echo === Running E2E Test: IntegrationTest ===
java -cp out grader.test.IntegrationTest
if %ERRORLEVEL% neq 0 exit /b 1

echo.
echo === Tests Passed Successfully ===

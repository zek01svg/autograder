#!/bin/bash
# Test runner for the AutoGrader project

echo "=== Compiling Project and Tests ==="
mkdir -p out
javac -d out -sourcepath src:tests src/grader/Main.java tests/grader/test/GraderTest.java tests/grader/test/IntegrationTest.java
if [ $? -ne 0 ]; then
    echo "Compilation FAILED."
    exit 1
fi

echo ""
echo "=== Running Unit Tests: GraderTest ==="
java -cp out grader.test.GraderTest
if [ $? -ne 0 ]; then exit 1; fi

echo ""
echo "=== Running E2E Test: IntegrationTest ==="
java -cp out grader.test.IntegrationTest
if [ $? -ne 0 ]; then exit 1; fi

echo ""
echo "=== Tests Passed Successfully ==="

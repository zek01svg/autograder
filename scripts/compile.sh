#!/bin/bash
# Compile all Java sources in src/ into out/ directory.
echo "Compiling..."
mkdir -p out
javac -d out -sourcepath src src/grader/Main.java src/grader/web/WebMain.java
if [ $? -ne 0 ]; then
    echo "Compilation FAILED."
    exit 1
fi
# Copy web assets
mkdir -p out/web
cp src/grader/web/index.html out/web/
echo "Compilation successful. Output in out/"

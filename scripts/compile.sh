#!/bin/bash
# Compile all Java sources in src/ into out/ directory.
echo "Compiling..."
mkdir -p out
javac -d out -sourcepath src $(find src -name "*.java")
if [ $? -ne 0 ]; then
    echo "Compilation FAILED."
    exit 1
fi
echo "Compilation successful. Output in out/"

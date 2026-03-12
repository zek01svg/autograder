#!/bin/bash
# Run the autograder Main class from out/ directory.
# Usage: ./scripts/run.sh --validate-only --submissions <folder> --workdir <folder>
java -cp out grader.Main "$@"

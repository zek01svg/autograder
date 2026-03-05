@echo off
REM Run the autograder Main class from out\ directory.
REM Usage: scripts\run.bat --validate-only --submissions <folder> --workdir <folder>
java -cp out Main %*

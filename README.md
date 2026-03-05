# OOP IS442 G3T3 — AutoGrader

A Java console application that auto-grades student ZIP submissions.

## Project Structure

```
src/
  Main.java              Entry point (CLI argument parsing)
  Validator.java         ZIP extraction + structure validation
  ValidationResult.java  Per-submission validation outcome
  Runner.java            Compile & execute (stub)
  Grader.java            Grade submissions (stub)
  Reporter.java          CSV report generator (stub)
  CsvUtil.java           CSV utility helpers (stub)
  GradeResult.java       Grading result data class (stub)
scripts/
  compile.bat            Compile all sources → out/
  run.bat                Run Main from out/ with forwarded args
```

## Quick Start

### 1. Compile

```bat
scripts\compile.bat
```

### 2. Run Validate-Only Mode

```bat
scripts\run.bat --validate-only --submissions <submissions-folder> --workdir <work-folder>
```

**Example:**
```bat
scripts\run.bat --validate-only --submissions C:\submissions --workdir work
```

### 3. Run Full Pipeline (stubs)

```bat
scripts\run.bat --submissions <submissions-folder>
```

## Expected Submission Format

Each student submits a ZIP file named `<username>.zip` containing:

```
<username>/
  Q1/
    Q1a.java
    Q1b.java
  Q2/
    Q2.java
  Q3/
    Q3.java
```

## Validation Checks

| # | Check | Severity |
|---|-------|----------|
| A | Username folder exists (detects `RenameToYourUsername`, flat hierarchy) | Anomaly |
| B | Required Q folders and `.java` files present | Anomaly |
| C | Java file headers contain name/email (first 15 lines) | Warning |
| D | Double-nested folder detection (`user/user/Q1/...`) | Warning |

## Console Output Format

```
[1/4] alice: OK
[2/4] bob: FAIL
    ANOMALY: Folder not renamed: found 'RenameToYourUsername' instead of 'bob'
    WARNING: Header missing name/email in Q1\Q1a.java
[3/4] charlie: FAIL
    ANOMALY: Missing username folder; incorrect hierarchy (Q1/Q2/Q3 placed directly at zip root)
    ANOMALY: Missing Q2 folder
[4/4] diana: OK
    WARNING: Header missing name/email in Q3\Q3.java

=== Validation Summary ===
Total : 4
Passed: 2
Failed: 2
```

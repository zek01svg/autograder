# OOP IS442 G3T3 — AutoGrader

A Java-based auto-grader for IS442 student submissions. Runs each test in an isolated Docker container and produces per-question scores and a gradebook-ready CSV. Instructors can interact via the **Next.js dashboard** (recommended) or the **CLI** directly.

---

## Prerequisites

- **JDK 17+**
- **Docker Desktop** (engine must be running)
- **Node.js 18+** and **pnpm** — for the dashboard only

---

## Option 1: Dashboard (Recommended)

A web UI with two modes:

- **Direct** — upload student submission zips, tester files folder, and exam template folder. Grade immediately.
- **Generate** — upload the question paper (PDF or text) and the template folder; the AI generates JUnit test files for you to review before grading.

### Setup

```sh
# 1. Compile the Java grader
./scripts/compile.sh        # macOS/Linux
scripts\compile.bat         # Windows

# 2. Install dashboard dependencies
cd dashboard
pnpm install

# 3. Start the dashboard
pnpm dev
```

Then open [http://localhost:3000](http://localhost:3000).

> Docker must be running before you click **Start Execution** in the dashboard.

### Dashboard workflow (Direct mode)

1. Select student `.zip` files (one per student)
2. Select the `Tester-Files` folder containing `*Tester.java` files
3. Select the `RenameToYourUsername` template folder
4. Click **Upload & Prepare**, then **Start Execution**
5. View per-question scores, validation status, and download the results CSV

### Dashboard workflow (Generate mode)

1. Paste or upload the question paper (PDF / `.txt` / `.md`)
2. Select the `RenameToYourUsername` template folder
3. Click **Start Autograder** — AI generates JUnit test files
4. Review and approve the generated tests
5. Click **Start Execution**
6. View results and download CSV

---

## Option 2: CLI

For scripted or headless environments.

### Build

```sh
./scripts/compile.sh        # macOS/Linux
scripts\compile.bat         # Windows
```

### Run

```sh
# macOS/Linux
./scripts/run.sh --submissions <path-to-zip-folder>

# Windows
scripts\run.bat --submissions <path-to-zip-folder>
```

### CLI flags

| Flag | Description | Default |
|---|---|---|
| `--submissions <path>` | **Required.** Directory containing student `.zip` files | — |
| `--testers <path>` | Directory containing `*Tester.java` files | `Tester-Files` |
| `--template <path>` | Template folder for structural validation | `RenameToYourUsername` |
| `--scoresheet <path>` | IS442 ScoreSheet CSV template | `scoresheet.csv` |
| `--output <path>` | Output CSV path | `results/results.csv` |
| `--workdir <path>` | Temp directory for extraction and compilation | `work` |
| `--validate-only` | Validate structure only, skip Docker execution | `false` |

### Other scripts

| Script | Description |
|---|---|
| `scripts/compile` | Compile all Java source files into `out/` |
| `scripts/test` | Run unit tests and E2E integration tests |

---

## Project Structure

```
src/grader/
  Main.java                  CLI entry point
  core/
    GradingPipeline.java     Primary orchestrator
    Runner.java              Docker execution engine
    Validator.java           Submission quality gate
    Grader.java              Score parsing logic
  model/
    GradeResult.java         Score data model
    ValidationResult.java    Submission health model
  report/
    Reporter.java            HTML & CSV report generation
    ProgressBar.java         CLI progress feedback
  util/
    FileUtil.java            Filesystem utilities
    CsvUtil.java             CSV parsing utilities
dashboard/                   Next.js web UI
tests/                       Unit & integration tests
results/                     Generated outputs (HTML report, CSV)
RenameToYourUsername/        Exam template folder
Tester-Files/                Tester Java files
config.properties            System configuration
```

---

## Configuration (`config.properties`)

| Key | Description | Default |
|---|---|---|
| `runner.threads` | Max concurrent Docker containers | `5` |
| `runner.memory` | Memory limit per container | `512m` |
| `runner.cpus` | CPU limit per container | `1.0` |
| `runner.timeout_seconds` | Execution timeout per student (seconds) | `15` |
| `dir.testers` | Tester files directory | `Tester-Files` |
| `dir.work` | Working directory for extractions | `work` |

---

## System Architecture

### Class Diagram

```mermaid
classDiagram
    namespace grader {
        class Main
    }
    namespace grader_core {
        class GradingPipeline
        class Validator
        class Runner
        class Grader
    }
    namespace grader_report {
        class Reporter
        class ProgressBar
    }
    namespace grader_model {
        class GradeResult
        class ValidationResult
    }
    namespace grader_util {
        class CsvUtil
        class FileUtil
    }

    Main ..> GradingPipeline : initiates
    GradingPipeline *-- Validator : uses
    GradingPipeline *-- Runner : uses
    GradingPipeline *-- Grader : uses
    GradingPipeline *-- Reporter : uses
    GradingPipeline *-- ProgressBar : uses

    Runner ..> RunOutput : produces
    Validator ..> ValidationResult : produces
    Reporter ..> CsvUtil : uses
```

### Execution Flow

```mermaid
flowchart TD
    subgraph Input ["Inputs"]
        ZIPs[".zip Submissions"]
        Config["config.properties"]
        Testers["Java Tester Files"]
    end

    subgraph UI ["Interface"]
        Dashboard["Next.js Dashboard"]
        CLI["CLI (grader.Main)"]
    end

    subgraph Core ["Orchestration Layer"]
        Pipeline["GradingPipeline"]
        Validator["Validator (Security & Structure)"]
    end

    subgraph Execution ["Isolated Execution Engine"]
        Runner["Runner (Thread Pool)"]
        Docker["Docker Container (1 Per Question)"]
    end

    subgraph Output ["Processing & Results"]
        Grader["Grader (Stdout Parsing)"]
        Reporter["Reporter"]
        Results["results/report.html + results.csv"]
    end

    Config -- "1. Load" --> Pipeline
    Dashboard -- "2. Spawn" --> CLI
    CLI -- "2. Initialize" --> Pipeline
    ZIPs -- "3. Scan" --> Validator
    Validator -- "3. Validated" --> Pipeline
    Pipeline -- "4. Submit Task" --> Runner
    Runner -- "5. Spin Up" --> Docker
    Testers -- "5. Compile & Run" --> Docker
    Docker -- "6. STDOUT" --> Grader
    Grader -- "6. Parse Score" --> Pipeline
    Pipeline -- "7. Aggregate" --> Reporter
    Reporter -- "7. Generate" --> Results

    style Dashboard fill:#6366f1,color:#fff,stroke:#4338ca
    style CLI fill:#f9f,stroke:#333
    style Pipeline fill:#b2e2f2,stroke:#333
    style Docker fill:#ffff00,stroke:#333
    style Results fill:#dae8fc,stroke:#6c8ebf
```

# ☕ AutoGrader Core (Java)

This directory contains the core Java logic for the AutoGrader. It handles submission validation, Docker-based test execution, and result report generation.

---

## 🛠 Prerequisites

- **JDK 17+**
- **Docker Desktop** (Engine must be running)

---

## 🚀 CLI Usage

For scripted or headless environments, you can interact with the core directly via the CLI.

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

### CLI Flags

| Flag | Description | Default |
|---|---|---|
| `--submissions <path>` | **Required.** Directory containing student `.zip` files | — |
| `--testers <path>` | Directory containing `*Tester.java` files | `Tester-Files` |
| `--template <path>` | Template folder for structural validation | `RenameToYourUsername` |
| `--output <path>` | Output CSV path | `results/results.csv` |
| `--workdir <path>` | Temp directory for extraction and compilation | `work` |
| `--validate-only` | Validate structure only, skip Docker execution | `false` |

---

## 📂 Project Structure

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
```

---

## ⚙️ Configuration (`config.properties`)

The core execution engine is configured via the `config.properties` file in the project root.

| Key | Description | Default |
|---|---|---|
| `runner.threads` | Max concurrent Docker containers | `10` |
| `runner.memory` | Memory limit per container | `256m` |
| `runner.cpus` | CPU limit per container | `0.5` |
| `runner.timeout_seconds` | Execution timeout per student (seconds) | `15` |
| `dir.testers` | Tester files directory | `Tester-Files` |
| `dir.work` | Working directory for extractions | `work` |

---

## 🏗 System Architecture

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

    subgraph Output ["Processing & Results"]
        Grader["Grader (Stdout Parsing)"]
        Reporter["Reporter"]
        Results["results/report.html + results.csv"]
    end

    subgraph AI ["AI Test Generation (Local)"]
        Ollama["Ollama (qwen2.5-coder)"]
        Schema["Simplified Stabilization Protocol"]
    end

    Config -- "1. Load" --> Pipeline
    Dashboard -- "2. Spawn" --> CLI
    CLI -- "2. Initialize" --> Pipeline
    ZIPs -- "3. Scan" --> Validator
    Validator -- "3. Validated" --> Pipeline
    
    Dashboard -- "AI Generation Request" --> Ollama
    Ollama -- "Structured JSON" --> Schema
    Schema -- "Approved Tests" --> Testers
    
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

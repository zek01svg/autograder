# 🎓 OOP IS442 G3T3 — AutoGrader

A modern Java-based autograder with AI-assisted test generation. Supports isolated Docker execution, real-time feedback, and automated gradebook generation.

## ✨ Features
- **Isolated Execution:** Every student's code runs in a clean Docker container.
- **AI-Assisted (Local):** Generate complex test suites from question papers using local LLMs (Ollama).
- **Two Modes:** 
    - **Direct:** Manual upload of pre-written tests.
    - **Generate:** AI-driven test creation and review.
- **Granular Reporting:** HTML reports with compilation/runtime error tooltips.

---

## 🛠 Prerequisites
- **JDK 17+**
- **Docker Desktop**
- **Node.js 24+ & pnpm v10.33**
- **Ollama** (For AI features)

---

## 🚀 Quick Start

1. **Compile the engine:**
   ```bash
   scripts/compile.bat  # Windows
   ./scripts/compile.sh # macOS/Linux
   ```

2. Add tester files, exam template, and student submissions to the `Tester-Files`, `RenameToYourUsername`, and `student-submission` folders respectively.

3. **Start the Dashboard:**
   ```bash
   cd dashboard
   pnpm install
   pnpm dev
   ```

3. **Open:** [http://localhost:3000](http://localhost:3000)

---

## 📖 Documentation

For detailed guides, please refer to the `docs/` directory:

- **[Installation & Setup](docs/setup.md)**
- **[Usage Guide (Direct & AI Modes)](docs/usage.md)**
- **[Architecture & Design](docs/architecture.md)**
- **[Configuration Reference](docs/configuration.md)**
- **[Troubleshooting FAQ](docs/troubleshooting.md)**

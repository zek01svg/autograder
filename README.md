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

1. **Model Setup (for AI Generation):**
   Ensure Ollama is installed and running, then pull the required model:
   ```bash
   ollama pull qwen2.5-coder:3b
   ```

2. **Compile the engine:**
   ```bash
   scripts/compile.bat  # Windows
   ./scripts/compile.sh # macOS/Linux
   ```

3. **Start the Dashboard:**
   ```bash
   cd dashboard
   pnpm install
   pnpm dev
   ```

4. **Open:** [http://localhost:3000](http://localhost:3000)

> [!TIP]
> You can either place files manually in the `Tester-Files/`, `web-uploads/template/`, and `web-uploads/submissions/` directories, or simply upload them using the **Direct** or **Generate** modes in the dashboard UI.

---

## 📖 Documentation

For detailed guides, please refer to the `docs/` directory:

- **[Installation & Setup](docs/setup.md)**
- **[Usage Guide (Direct & AI Modes)](docs/usage.md)**
- **[Architecture & Design](docs/architecture.md)**
- **[Configuration Reference](docs/configuration.md)**
- **[Troubleshooting FAQ](docs/troubleshooting.md)**

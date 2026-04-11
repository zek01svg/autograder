# 🛠 Installation & Setup

This guide covers the necessary steps to get the AutoGrader up and running on your local machine.

## 📋 Prerequisites

| Tool | Why it's needed |
|------|----------------|
| **☕ JDK 17+** | Compiles and runs the Java grading engine |
| **🐳 Docker Desktop** | Runs student code in isolated containers |
| **📦 Node.js 18+** & **pnpm** | Powers the Next.js instructor dashboard |
| **🤖 Ollama** | (Optional) Required for AI-assisted test generation |

---

## ☕ Java Engine Setup

The grading engine must be compiled before use:

```bash
# Windows
scripts/compile.bat

# macOS / Linux
./scripts/compile.sh
```

---

## 🎨 Dashboard Setup

Install dependencies and start the web interface:

```bash
cd dashboard
pnpm install
pnpm dev
```

> **Note:** If you don't have `pnpm` installed, run `npm install -g pnpm` first.

---

## 🤖 Ollama Setup (Generate Mode Only)

If you plan to use AI-assisted test generation, you must install Ollama and pull the required model.

### 1. Install Ollama
Download and install from [ollama.com/download](https://ollama.com/download).

### 2. Verify Ollama is running
```bash
ollama version
```

### 3. Pull the Model
Once installed and running, download the coding model:

```bash
ollama pull qwen2.5-coder:3b
```

---

## 🚀 Service Checklist

Before starting a grading session, ensure:
1. **Docker Desktop** is running.
2. **Ollama** is running (if using Generate mode).
3. The Java engine is **compiled**.

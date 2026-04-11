# 📖 Usage Guide

The AutoGrader supports two primary workflows via the **Dashboard** and a headless **CLI** mode for advanced users.

---

## 🎨 Dashboard Workflows

### 📤 Mode 1: Direct (Manual Testing)
Use this mode when you already have `*Tester.java` files.

**Preparation Table:**
| Upload slot | What to select | Description |
|---|---|---|
| **Submissions** | Zip Folder | A folder containing student `.zip` files. |
| **Tester Files** | Tester Folder | The folder containing your `*Tester.java` files. |
| **Exam Template** | Template Folder | The `RenameToYourUsername/` folder defining the expected structure. |

**Steps:**
1. Upload the three required folders.
2. Click **Upload & Prepare**.
3. Click **Start Execution**.
4. View results in the table and download the CSV.

---

### 🧬 Mode 2: Generate (AI-Assisted)
Use this mode when you need the AI to generate tests from a question paper.

**Steps:**
1. Upload your question paper (PDF/TXT/MD).
2. Select the exam template folder.
3. Click **Generate Tests** (uses local Ollama).
4. Review and edit the generated code in the editor.
5. Click **Commit All Tests** to save and begin execution.

---

## 💻 CLI Usage (Advanced)

The core engine can be run directly from the terminal without the dashboard.

### Commands
```bash
# Windows
scripts\run.bat --submissions <path-to-zips>

# macOS / Linux
./scripts/run.sh --submissions <path-to-zips>
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

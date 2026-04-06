# AutoGrader Dashboard

Next.js web UI for the IS442 AutoGrader. Provides two grading workflows and displays per-question results with score distribution charts.

---

## Prerequisites

- **Node.js 18+**
- **pnpm**
- Java grader compiled (`../out/` must exist — run `scripts/compile` from the project root)
- **Docker Desktop** running (required when grading executes)

---

## Setup

```sh
pnpm install
pnpm dev
```

Open [http://localhost:3000](http://localhost:3000).

---

## Workflows

### Direct mode (bring your own tests)

Upload three things, then grade:

| Input         | What to select                                                               |
| ------------- | ---------------------------------------------------------------------------- |
| Submissions   | Student `.zip` files (one per student)                                       |
| Tester Files  | The `Tester-Files/` folder containing `*Tester.java` files                   |
| Exam Template | The `RenameToYourUsername/` folder containing `Q1/`, `Q2/`, `Q3/` subfolders |

Click **Upload & Prepare**, then **Start Execution**.

### Generate mode (AI-assisted)

For when tester files don't exist yet:

1. Paste the question paper or upload a PDF / `.txt` / `.md`
2. Select the `RenameToYourUsername/` template folder
3. Click **Start Autograder** — Gemini generates JUnit test files
4. Review and edit the generated tests in the editor
5. Click **Save & Continue**, then **Start Execution**

---

## Architecture

```mermaid
flowchart LR
    subgraph Browser
        UI["Next.js UI"]
    end

    subgraph Next.js API Routes
        Upload["/api/upload\nSave & reconstruct files"]
        Grade["/api/grade\nSpawn Java grader"]
        Run["/api/run\nSpawn Java grader (AI path)"]
        Generate["/api/generate\nGemini AI → JUnit tests"]
        Save["/api/save\nWrite tests to disk"]
        Results["/api/results\nParse CSV + HTML"]
    end

    subgraph Java Grader
        Main["grader.Main"]
        Pipeline["GradingPipeline"]
        Docker["Docker containers"]
    end

    subgraph Filesystem
        WebUploads["web-uploads/"]
        ResultsDir["results/\nresults.csv\nreport.html"]
    end

    UI -- "Direct mode" --> Upload --> WebUploads
    UI -- "Start Execution (Direct)" --> Grade
    UI -- "Start Execution (AI)" --> Run
    UI -- "Generate mode" --> Generate --> Save
    Grade --> Main
    Run --> Main
    Main --> Pipeline --> Docker
    Docker --> ResultsDir
    UI -- "Fetch results" --> Results
    Results --> ResultsDir
```

---

## API Routes

| Route           | Method | Description                                                                                 |
| --------------- | ------ | ------------------------------------------------------------------------------------------- |
| `/api/upload`   | POST   | Accepts multipart form data (`submissions`, `testers`, `template`), saves to `web-uploads/` |
| `/api/grade`    | POST   | Streams output from `grader.Main` using the `web-uploads/` directories                      |
| `/api/run`      | POST   | Streams output from `grader.Main` using the saved AI-generated tests                        |
| `/api/generate` | POST   | Sends question paper to Gemini, returns generated JUnit test files                          |
| `/api/save`     | POST   | Writes approved test files to `tests/`                                                      |
| `/api/results`  | GET    | Reads `results/results.csv` and `results/report.html`, returns parsed JSON                  |

---

## Environment Variables

Create a `.env.local` in this directory:

```env
GEMINI_API_KEY=your_api_key_here
```

Required only for Generate mode.

---

## Project Structure

```mermaid
graph TD
    src["src/"]
    app["app/"]
    api["api/"]
    components["components/"]
    lib["lib/"]

    src --> app
    src --> components
    src --> lib

    app --> api
    app --> page["page.tsx — main UI"]
    app --> layout["layout.tsx"]

    api --> upload["upload/ — file upload handler"]
    api --> grade["grade/ — direct grading stream"]
    api --> run["run/ — AI-path grading stream"]
    api --> generate["generate/ — AI test generation"]
    api --> save["save/ — write tests to disk"]
    api --> results["results/ — parse & serve results"]

    components --> UploadZone["UploadZone.tsx — Direct/Generate upload UI"]
    components --> TestReviewer["TestReviewer.tsx — AI test review editor"]
    components --> ScoreDistribution["ScoreDistribution.tsx — Recharts bell curve"]

    lib --> schema["schema.ts — Zod types"]
    lib --> utils["utils.ts — question paper splitting"]
    lib --> ai["ai.ts — Gemini client"]
```

# 🎨 AutoGrader Dashboard

The instructor interface for the IS442 AutoGrader. Built with Next.js, Tailwind CSS, and local AI integration.

## 🚀 Setup

```sh
# Run the development server
pnpm install --frozen-lockfile
pnpm dev

# Run the production server
pnpm turbo run build
pnpm start
```

Open [http://localhost:3000](http://localhost:3000).

## 📖 Key Features
- **Project Upload:** drag-and-drop support for student zips and template folders.
- **AI Test Generation:** Streams test cases directly from local Ollama instances.
- **Interactive Review:** Built-in code editor for reviewing and tweaking AI-generated tests.
- **Live Execution Logs:** Real-time feedback from the Java core engine.

## 🔗 Further Documentation
Comprehensive documentation is available in the root **[docs/](../docs/)** folder.

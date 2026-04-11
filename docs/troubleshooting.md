# 🔧 Troubleshooting

Common issues and their resolutions.

## 🔧 FAQ Table

| Problem | Solution |
|---------|----------|
| Docker errors during grading | Ensure Docker Desktop is running. Try `docker ps` in terminal to verify connection. |
| Ollama connection refused | Ensure Ollama is running. On Windows/macOS, check the system tray icon. On Linux, run `sudo systemctl start ollama`. |
| "Ollama is running" but model not found | Run `ollama pull qwen2.5-coder:3b`. |
| Java compilation fails | Verify JDK 17+ installation with `java -version`. Ensure `javac` is on your PATH. |
| `pnpm: command not found` | Install via npm: `npm install -g pnpm`. |
| Port 3000 already in use | Kill the process using the port. On Windows: `netstat -ano \| findstr :3000` then `taskkill /PID <pid> /F`. |
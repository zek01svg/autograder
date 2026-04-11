# ⚙️ Configuration Properties

The core grading engine behavior is managed through the `config.properties` file in the project root.

## ⚙️ Properties Table

| Key | Description | Default |
|-----|-------------|---------|
| `runner.threads` | Maximum concurrent Docker containers. | `10` |
| `runner.memory` | Memory limit per container (e.g., `256m`, `1g`). | `256m` |
| `runner.cpus` | CPU limit per container (decimal). | `0.5` |
| `runner.timeout_seconds` | Maximum execution time per student before force stop. | `15` |
| `dir.testers` | Directory name where tester files are located. | `Tester-Files` |
| `dir.work` | Directory name for temporary code extraction. | `work` |

---

## 🏗 Resource Limits

We recommend keeping `runner.threads` low (e.g., 5-10) if your machine has limited memory, as each thread spawns a separate Docker container running a JVM.

- **CPU Limit:** `0.5` ensures no single submission can monopolize the host CPU.
- **Memory Limit:** `256m` is sufficient for most standard student solutions. Increase this only if students are expected to process large datasets.

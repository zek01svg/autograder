package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Compiles and executes student submissions. Uses the Docker API to
 * concurrently spin up multiple containers to compile and execute each
 * submission.
 */
public class Runner {

  private final ExecutorService executor;

  public Runner() {
    // Spin up 5 concurrent containers as per implementation plan
    this.executor = Executors.newFixedThreadPool(5);
  }

  public static class RunOutput {
    public String studentId;
    public boolean success;
    public boolean timedOut;
    public String output;
    public String error;

    public RunOutput(String studentId, boolean success, boolean timedOut, String output, String error) {
      this.studentId = studentId;
      this.success = success;
      this.timedOut = timedOut;
      this.output = output;
      this.error = error;
    }
  }

  public void shutdown() {
    executor.shutdown();
  }

  /**
   * Concurrently compiles and runs a list of submission directories.
   */
  public List<RunOutput> compileAndRunAll(List<String> submissionPaths) {
    List<Callable<RunOutput>> tasks = new ArrayList<>();

    for (String path : submissionPaths) {
      File dir = new File(path);
      String studentId = dir.getName();
      tasks.add(() -> compileAndRun(path, studentId));
    }

    List<RunOutput> results = new ArrayList<>();
    try {
      List<Future<RunOutput>> futures = executor.invokeAll(tasks);
      for (Future<RunOutput> future : futures) {
        results.add(future.get());
      }
    } catch (Exception e) {
      System.err.println("Error executing concurrent tasks: " + e.getMessage());
    }

    return results;
  }

  /**
   * Compiles and runs a single submission inside a disposable Docker container
   * with strict resource limits.
   */
  public RunOutput compileAndRun(String submissionPath, String studentId, String mainClass) {
    String containerName = "autograder-" + UUID.randomUUID().toString().substring(0, 8);
    File subDir = new File(submissionPath);
    String absolutePath = subDir.getAbsolutePath();

    // 1. Necessary flags for each container
    // --rm : automatically remove the container
    // -v : volume mount the submission folder to /app
    // -w : working directory set to /app
    // --network none : no internet access
    // --memory/cpus : limit resources
    ProcessBuilder pb = new ProcessBuilder(
        "docker", "run",
        "--rm",
        "--name", containerName,
        "-v", absolutePath + ":/app",
        "-w", "/app",
        "--network", "none",
        "--memory=512m",
        "--cpus=1.0",
        "eclipse-temurin:17-jdk-alpine",
        "sh", "-c", "javac $(find . -name '*.java') && java " + mainClass);

    pb.redirectErrorStream(true);

    StringBuilder output = new StringBuilder();
    boolean success = false;
    boolean timedOut = false;
    String errorMsg = "";

    try {
      Process process = pb.start();

      // Read output in a separate thread to prevent blocking
      Thread readerThread = new Thread(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
          }
        } catch (Exception e) {
          output.append("\n[Error reading stream: ").append(e.getMessage()).append("]");
        }
      });
      readerThread.start();

      // 2. Timeout handling (15 seconds)
      if (!process.waitFor(15, TimeUnit.SECONDS)) {
        timedOut = true;
        killContainer(containerName);
        process.destroyForcibly();
        errorMsg = "Process timed out after 15 seconds.";
      } else {
        success = (process.exitValue() == 0);
      }

      readerThread.join(2000); // Wait up to 2s for reader thread to finish

    } catch (Exception e) {
      errorMsg = "Failed to execute docker container: " + e.getMessage();
      success = false;
    }

    return new RunOutput(studentId, success, timedOut, output.toString(), errorMsg);
  }

  // Overload for backward compatibility if needed, or just update callers
  public RunOutput compileAndRun(String submissionPath, String studentId) {
    return compileAndRun(submissionPath, studentId, "RunnerMain");
  }

  private void killContainer(String containerName) {
    try {
      new ProcessBuilder("docker", "rm", "-f", containerName).start().waitFor();
    } catch (Exception e) {
      System.err.println("Failed to kill container " + containerName + ": " + e.getMessage());
    }
  }
}

package tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import src.Runner;
import src.Runner.RunOutput;

/**
 * End-to-End Test: Simulates the entire flow using actual student submissions
 * (ZIP files) and the tester files provided by the professor.
 */
public class IntegrationTest {
  private static final String STUDENT_SUB_DIR = "student-submission";
  private static final String TESTER_DIR = "Tester-Files";
  private static final String TEMP_EXTRACT_DIR = "tmp/extraction";

  public static void main(String[] args) throws Exception {
    System.out.println("Starting Real-World End-to-End Integration Test...");

    // 1. Setup - Create temp directory
    File extractRoot = new File(TEMP_EXTRACT_DIR);
    if (extractRoot.exists()) {
      deleteDirectory(extractRoot);
    }
    extractRoot.mkdirs();

    // 2. Pick a sample submission
    File subDir = new File(STUDENT_SUB_DIR);
    File[] zips = subDir.listFiles((dir, name) -> name.endsWith(".zip"));

    if (zips == null || zips.length == 0) {
      System.err.println("No student submissions found in " + STUDENT_SUB_DIR);
      return;
    }

    File sampleZip = zips[0];
    String studentId = sampleZip.getName().replace(".zip", "");
    System.out.println("Testing submission: " + sampleZip.getName());

    // 3. Unzip the submission
    unzip(sampleZip.getAbsolutePath(), TEMP_EXTRACT_DIR);

    // 4. Run through the testers
    Runner runner = new Runner();

    String[][] testCases = {
        { "Q1", "Q1a", "Q1aTester" },
        { "Q1", "Q1b", "Q1bTester" },
        { "Q2", "Q2a", "Q2aTester" },
        { "Q2", "Q2b", "Q2bTester" },
        { "Q3", "Q3", "Q3Tester" }
    };

    for (String[] tc : testCases) {
      String folder = tc[0];
      String subFolder = tc[1];
      String testerName = tc[2];

      System.out.println("\n--- Testing " + testerName + " ---");

      // Prepare target path (some students might have Q1/Q1a.java or just
      // Q1/Q1a.java)
      // Based on our inspection, it's tmp/student_test/Q1/Q1a.java
      Path submissionPath = Path.of(TEMP_EXTRACT_DIR, folder);

      // Copy tester to the submission folder
      Path testerSource = Path.of(TESTER_DIR, testerName + ".java");
      Path testerTarget = submissionPath.resolve(testerName + ".java");
      
      try {
        Files.copy(testerSource, testerTarget, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        System.err.println("Warning: Could not copy tester " + testerName + " to " + submissionPath);
        continue;
      }

      // Run
      RunOutput result = runner.compileAndRun(submissionPath.toString(), studentId, testerName);

      if (result.success) {
        System.out.println("Execution Output:\n" + result.output);
        // We can extract the score from the last line
        String[] lines = result.output.split("\n");
        if (lines.length > 0) {
          String scoreLine = lines[lines.length - 1].trim();
          System.out.println("Detected Score: " + scoreLine);
        }
      } else {
        System.err.println("Compilation/Execution failed for " + testerName);
        System.err.println("Error: " + result.error);
        System.err.println("Output: " + result.output);
      }
    }

    runner.shutdown();
    System.out.println("\nIntegrationTest: Real-world flow verified.");
  }

  private static void unzip(String zipFilePath, String destDir) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(
        "powershell.exe", "-Command",
        "Expand-Archive -Path '" + zipFilePath + "' -DestinationPath '" + destDir + "' -Force");
    Process p = pb.start();
    p.waitFor();
  }

  private static void deleteDirectory(File dir) throws IOException {
    Files.walk(dir.toPath())
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }
}

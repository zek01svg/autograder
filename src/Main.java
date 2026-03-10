package src;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the console auto-grading system.
 *
 * Usage:
 * java Main --validate-only --submissions &lt;folder&gt; --workdir
 * &lt;folder&gt;
 *
 * Modes:
 * --validate-only Scan for .zip files, validate each, print summary.
 * (default) Full pipeline: validate → compile → grade → report. (stubs)
 */
public class Main {

  public static void main(String[] args) {
    // --- Parse CLI arguments ---
    String mode = "full";
    String submissionsDir = null;
    String workDir = null;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--validate-only":
          mode = "validate-only";
          break;
        case "--submissions":
          if (i + 1 < args.length)
            submissionsDir = args[++i];
          break;
        case "--workdir":
          if (i + 1 < args.length)
            workDir = args[++i];
          break;
        default:
          // Legacy: first positional arg = submissions dir
          if (submissionsDir == null) {
            submissionsDir = args[i];
          }
          break;
      }
    }

    if (submissionsDir == null) {
      printUsageAndExit();
    }
    if (workDir == null) {
      workDir = "work"; // default working directory
    }

    System.out.println("=== AutoGrader ===");
    System.out.println("Submissions : " + submissionsDir);
    System.out.println("Work dir    : " + workDir);
    System.out.println("Mode        : " + mode);
    System.out.println();

    if (mode.equals("validate-only")) {
      runValidateOnly(submissionsDir, workDir);
    } else {
      runFullPipeline(submissionsDir, workDir);
    }
  }

  // ---------------------------------------------------------------- Modes

  /**
   * Validate-only mode: scan for zip files, validate each, print summary.
   */
  private static void runValidateOnly(String submissionsDir, String workDir) {
    File dir = new File(submissionsDir);
    if (!dir.exists() || !dir.isDirectory()) {
      System.err.println("ERROR: Submissions directory not found: " + submissionsDir);
      System.exit(1);
    }

    // Collect all .zip files
    File[] zipFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip"));
    if (zipFiles == null || zipFiles.length == 0) {
      System.out.println("No .zip files found in " + submissionsDir);
      return;
    }

    System.out.println("Found " + zipFiles.length + " submission(s).\n");

    Validator validator;
    try {
      validator = new Validator("requirements.txt");
    } catch (java.io.IOException e) {
      System.err.println("ERROR: Could not load configuration file 'requirements.txt': " + e.getMessage());
      System.exit(1);
      return;
    }
    ArrayList<ValidationResult> results = new ArrayList<>();
    int passCount = 0;

    for (int i = 0; i < zipFiles.length; i++) {
      ValidationResult vr;
      try {
        vr = validator.validateZipSubmission(
            zipFiles[i].getAbsolutePath(), workDir);
      } catch (Exception e) {
        // Safety net: one bad submission must never crash the batch
        String sid = Validator.deriveStudentIdFromZip(zipFiles[i].getName());
        vr = new ValidationResult(sid);
        vr.setOk(false);
        vr.getAnomalies().add("Unexpected error: " + e.getMessage());
      }
      results.add(vr);

      // Print per-submission summary
      String status = vr.isOk() ? "OK" : "FAIL";
      System.out.println("[" + (i + 1) + "/" + zipFiles.length + "] "
          + vr.getStudentId() + ": " + status);

      if (!vr.isOk()) {
        for (String a : vr.getAnomalies()) {
          System.out.println("    ANOMALY: " + a);
        }
      }
      if (!vr.getWarnings().isEmpty()) {
        for (String w : vr.getWarnings()) {
          System.out.println("    WARNING: " + w);
        }
      }

      if (vr.isOk())
        passCount++;
    }

    // Summary
    System.out.println();
    System.out.println("=== Validation Summary ===");
    System.out.println("Total : " + results.size());
    System.out.println("Passed: " + passCount);
    System.out.println("Failed: " + (results.size() - passCount));
  }

  /**
   * Full grading pipeline — stubs for compile/grade/report phases.
   */
  private static void runFullPipeline(String submissionsDir, String workDir) {
    System.out.println("Starting full grading pipeline...");

    // 1. Validate
    System.out.println("[1/4] Validating submissions...");
    File dir = new File(submissionsDir);
    File[] zipFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip"));
    if (zipFiles == null || zipFiles.length == 0) {
      System.out.println("No .zip files found.");
      return;
    }

    Validator validator;
    try {
      validator = new Validator("requirements.txt");
    } catch (IOException e) {
      System.err.println("ERROR: Could not load configuration file 'requirements.txt': " + e.getMessage());
      return;
    }

    List<String> validSubmissionPaths = new ArrayList<>();
    for (File zip : zipFiles) {
      ValidationResult vr = validator.validateZipSubmission(zip.getAbsolutePath(), workDir);
      if (vr.isOk()) {
        validSubmissionPaths.add(vr.getExtractedRoot());
      } else {
        System.out.println("Skipping " + vr.getStudentId() + " due to validation failure.");
      }
    }

    // 2. Compile & Execute
    System.out.println("[2/4] Compiling and executing in Docker...");
    Runner runner = new Runner();
    List<Runner.RunOutput> runResults = runner.compileAndRunAll(validSubmissionPaths);

    // 3. Grade
    System.out.println("[3/4] Grading outputs...");
    Grader grader = new Grader();
    List<GradeResult> finalResults = new ArrayList<>();
    // Mock expected output for now as we don't have a lookup system
    String mockExpected = "Hello World\n";

    for (Runner.RunOutput runRes : runResults) {
      GradeResult gr = grader.grade(runRes.studentId, runRes.success, runRes.output, mockExpected);
      finalResults.add(gr);
      System.out.println("Result for " + runRes.studentId + ": " + gr.getScore() + "/100 - " + gr.getFeedback());
    }

    // 4. Report (Stub call)
    System.out.println("[4/4] Reporting... (Reporter still a stub)");
    // Reporter.writeCsv("results.csv", finalResults);

    runner.shutdown();
    System.out.println("\nAutoGrader pipeline finished.");
  }

  // ---------------------------------------------------------------- Helpers

  private static void printUsageAndExit() {
    System.err.println("Usage:");
    System.err.println("  java Main --validate-only --submissions <folder> [--workdir <folder>]");
    System.err.println("  java Main --submissions <folder> [--workdir <folder>]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  --validate-only   Run validation only (no compile/grade/report)");
    System.err.println("  --submissions     Path to folder containing student .zip files");
    System.err.println("  --workdir         Working directory for extraction (default: work)");
    System.exit(1);
  }
}

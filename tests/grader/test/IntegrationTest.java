package grader.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import grader.core.*;
import grader.model.*;
import grader.report.*;
import grader.util.*;

/**
 * End-to-End Test: Simulates the entire grading pipeline using actual student
 * submissions (ZIP files), tester files, and CSV reporting.
 *
 * Flow: Unzip → Copy Testers → Docker Run → Parse Scores → Report CSV
 */
public class IntegrationTest {
  private static final String STUDENT_SUB_DIR = "student-submission";
  private static final String TESTER_DIR = "Tester-Files";
  private static final String TEMP_EXTRACT_DIR = "tmp/extraction";
  private static final String SCORESHEET_PATH = "results.csv";
  private static final String OUTPUT_CSV_PATH = "tmp/test_results.csv";

  // Question-to-tester mapping: { folder, questionKey, testerClassName }
  private static final String[][] TEST_CASES = {
      { "Q1", "Q1a", "Q1aTester" },
      { "Q1", "Q1b", "Q1bTester" },
      { "Q2", "Q2a", "Q2aTester" },
      { "Q2", "Q2b", "Q2bTester" },
      { "Q3", "Q3", "Q3Tester" }
  };

  public static void main(String[] args) throws Exception {
    System.out.println("=== E2E Integration Test: Full Grading Pipeline ===\n");

    // 1. Setup
    FileUtil.deleteDirectory(new File(TEMP_EXTRACT_DIR)); // Use FileUtil for deletion
    File extractRoot = new File(TEMP_EXTRACT_DIR); // Re-create the File object for mkdirs
    extractRoot.mkdirs();
    new File("tmp").mkdirs(); // ensure tmp/ exists for output CSV

    // 2. Find all student submissions
    File subDir = new File(STUDENT_SUB_DIR);
    File[] zips = subDir.listFiles((dir, name) -> name.endsWith(".zip"));

    if (zips == null || zips.length == 0) {
      System.err.println("No student submissions found in " + STUDENT_SUB_DIR);
      return;
    }

    System.out.println("Found " + zips.length + " submission(s).\n");

    // Load Dummy Config
    java.util.Properties config = new java.util.Properties();
    config.setProperty("runner.threads", "3");
    config.setProperty("runner.timeout_seconds", "10");

    Runner runner = new Runner(config);
    Grader grader = new Grader();
    ArrayList<GradeResult> allResults = new ArrayList<>();

    // 3. Grade each submission
    ProgressBar progressBar = new ProgressBar("Grading", zips.length);
    List<Future<GradeResult>> futures = new ArrayList<>();
    for (File zip : zips) {
      futures.add(runner.submitTask(() -> {
        GradeResult r = processStudentInTest(zip, runner, grader);
        progressBar.advance();
        return r;
      }));
    }

    for (Future<GradeResult> future : futures) {
      try {
        allResults.add(future.get());
      } catch (Exception e) {
        System.err.println("Test worker failed: " + e.getMessage());
      }
    }
    progressBar.finish();

    runner.shutdown();

    // 4. Verify that we actually got some results (prevent false positives)
    boolean hasNonZeroScore = false;
    for (GradeResult r : allResults) {
      if (r.getTotalScore() > 0) {
        hasNonZeroScore = true;
        break;
      }
    }
    TestUtils.assertTrue("At least one student should have a non-zero score", hasNonZeroScore);

    // 5. Report: print detailed table and write CSV
    String[] qKeys = { "Q1a", "Q1b", "Q2a", "Q2b", "Q3" };
    Reporter reporter = new Reporter(qKeys);
    reporter.printDetailedReport(allResults);

    // Write results CSV (merging with the original scoresheet)
    File scoresheetFile = new File(SCORESHEET_PATH);
    if (scoresheetFile.exists()) {
      reporter.writeCsv(SCORESHEET_PATH, OUTPUT_CSV_PATH, allResults);
      System.out.println("\nResults CSV written to: " + OUTPUT_CSV_PATH);

      // Verify the CSV was written
      File outputFile = new File(OUTPUT_CSV_PATH);
      TestUtils.assertTrue("CSV output file should exist", outputFile.exists());
      TestUtils.assertTrue("CSV output file should not be empty", outputFile.length() > 0);

      // Print the CSV contents for verification
      System.out.println("\n=== CSV Output ===");
      java.util.List<String> csvLines = Files.readAllLines(outputFile.toPath());
      for (String line : csvLines) {
        System.out.println(line);
      }
    } else {
      System.out.println("\nWARNING: Scoresheet not found at " + SCORESHEET_PATH + ", skipping CSV output.");
    }

    System.out.println("\n=== E2E Integration Test: COMPLETE ===");
  }

  /**
   * Parse the numeric score from the last non-empty line of tester output.
   * Tester files print the score (e.g., "3.0") as their last line.
   */
  private static double parseScoreFromOutput(String output) {
    String[] lines = output.split("\n");
    for (int i = lines.length - 1; i >= 0; i--) {
      String line = lines[i].trim();
      if (!line.isEmpty()) {
        try {
          return Double.parseDouble(line);
        } catch (NumberFormatException e) {
          // Not a number, keep looking
        }
      }
    }
    return 0.0;
  }

  /**
   * Recursively find the directory that directly contains Q1/, Q2/, Q3/ folders.
   * Handles varying zip structures (direct, nested under OrgId, nested under
   * placeholder name).
   */
  private static GradeResult processStudentInTest(File zip, Runner runner, Grader grader) {
    String rawName = zip.getName().replace(".zip", "");
    String studentId = rawName.replaceFirst("^\\d{4}-\\d{4}-", "");
    GradeResult result = new GradeResult(studentId, true);

    try {

      String studentExtractDir = TEMP_EXTRACT_DIR + "/" + studentId;
      FileUtil.unzip(zip.getAbsolutePath(), new File(studentExtractDir));

      File submissionRoot = FileUtil.findSubmissionRoot(new File(studentExtractDir));
      if (submissionRoot == null) {
        System.err.println("  [" + studentId + "] SKIP: Question folders not found.");
        return result;
      }
      // 1. Prepare Testers and execute per-question
      for (String[] tc : TEST_CASES) {
        String folder = tc[0];
        String questionKey = tc[1];
        String testerName = tc[2];

        Path submissionPath = submissionRoot.toPath().resolve(folder);
        Path testerSource = Path.of(TESTER_DIR, testerName + ".java");
        Path testerTarget = submissionPath.resolve(testerName + ".java");

        try {
          Files.copy(testerSource, testerTarget, StandardCopyOption.REPLACE_EXISTING);
          String cmd = String.format("cd \"%s\" && javac *.java && java -cp . %s", folder, testerName);
          Runner.RunOutput runResult = runner.compileAndRun(
              submissionRoot.getAbsolutePath(), studentId, cmd);
          result.setQuestionScore(questionKey, grader.parseScoreFromOutput(runResult.output));
        } catch (IOException e) {
          result.setQuestionScore(questionKey, 0.0);
        }
      }

      // Progress is tracked by the caller's ProgressBar

    } catch (Exception e) {
      System.err.println("  [" + studentId + "] ERROR: " + e.getMessage());
    }
    return result;
  }

}


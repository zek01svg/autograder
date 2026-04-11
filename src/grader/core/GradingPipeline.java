package grader.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import grader.model.*;
import grader.report.*;
import grader.util.*;

/**
 * Orchestrates the grading process: Validation -> Unzip -> Execute -> Report.
 */
public class GradingPipeline {

  private final String submissionsDir;
  private final String testersDir;
  private final String scoresheetPath;
  private final String outputPath;
  private final String workDir;
  private final String templateDir;
  private final Properties config;
  private final Map<String, File> resolvedTesters = new HashMap<>();

  public GradingPipeline(String submissionsDir, String testersDir, String scoresheetPath,
      String outputPath, String workDir, String templateDir, Properties config) {
    this.submissionsDir = submissionsDir;
    this.testersDir = testersDir;
    this.scoresheetPath = scoresheetPath;
    this.outputPath = outputPath;
    this.workDir = workDir;
    this.templateDir = templateDir;
    this.config = config;
  }

  public boolean run() {
    System.out.println("Starting full grading pipeline...");

    // Clean work directory from previous runs to prevent stale file interference
    File workDirFile = new File(workDir);
    if (workDirFile.exists()) {
      System.out.println("Cleaning previous work directory: " + workDir);
      try {
        FileUtil.deleteDirectory(workDirFile);
      } catch (IOException e) {
        System.err.println("WARNING: Could not fully clean work directory: " + e.getMessage());
      }
    }

    // Check Prerequisites
    File subDir = new File(submissionsDir);
    if (!subDir.exists() || !subDir.isDirectory()) {
      System.err.println("ERROR: Submissions directory not found: " + submissionsDir);
      return false;
    }

    File testDir = new File(testersDir);
    if (!testDir.exists() || !testDir.isDirectory()) {
      System.err.println("ERROR: Testers directory not found: " + testersDir);
      return false;
    }

    File tempDir = new File(templateDir);
    if (!tempDir.exists() || !tempDir.isDirectory()) {
      System.err.println("ERROR: Template directory not found: " + templateDir);
      return false;
    }

    // 1. Validate
    System.out.println("[1/4] Validating submissions...");
    File[] zipFiles = subDir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip"));
    if (zipFiles == null || zipFiles.length == 0) {
      System.out.println("No .zip files found in " + submissionsDir);
      return false;
    }

    Validator validator;
    try {
      validator = new Validator(templateDir, true);
    } catch (IOException e) {
      System.err.println("ERROR: Could not load template from '" + templateDir + "': " + e.getMessage());
      return false;
    }

    List<File> validZips = new ArrayList<>();
    int invalidCount = 0;
    for (File zip : zipFiles) {
      ValidationResult vr = validator.validateZipSubmission(zip.getAbsolutePath(), workDir);
      validZips.add(zip); // always proceed
      if (!vr.isOk()) {
        invalidCount++;
      }
    }

    System.out.println("Submissions: " + zipFiles.length);
    System.out.println("Valid: " + (zipFiles.length - invalidCount));
    System.out.println("Invalid: " + invalidCount);

    // 2. Compile & Execute
    System.out.println("\n[2/4] Compiling and executing in Docker...");
    Runner runner = new Runner(config);

    if (!runner.isDockerAvailable()) {
      System.err.println("\n[!] CRITICAL ERROR: Docker engine is not running or accessible.");
      System.err.println("    The AutoGrader requires Docker to execute student code safely.");
      System.err.println("    Please start Docker Desktop and ensure 'docker info' works in your terminal.");
      runner.shutdown();
      return false;
    }

    // Pre-pull Docker image to avoid per-container timeout on first run
    String dockerImage = config.getProperty("runner.image", "eclipse-temurin:17-jdk");
    if (!runner.ensureImageAvailable(dockerImage)) {
      System.err.println("ERROR: Failed to pull Docker image: " + dockerImage);
      System.err.println("    Please run 'docker pull " + dockerImage + "' manually and try again.");
      runner.shutdown();
      return false;
    }

    Grader grader = new Grader();
    ArrayList<GradeResult> allResults = new ArrayList<>();
    String[][] testCases = buildTestCases(config.getProperty("questions", ""));
    if (testCases.length == 0) {
      System.err.println("ERROR: No test cases found. Aborting to avoid zeroed results.");
      runner.shutdown();
      return false;
    }

    ProgressBar progressBar = new ProgressBar("Grading", validZips.size());
    List<java.util.concurrent.Future<GradeResult>> futures = new ArrayList<>();
    for (File zip : validZips) {
      futures.add(runner.submitTask(() -> {
        GradeResult r = processStudent(zip, testCases, runner, grader);
        progressBar.advance();
        return r;
      }));
    }

    for (java.util.concurrent.Future<GradeResult> future : futures) {
      try {
        allResults.add(future.get());
      } catch (Exception e) {
        System.err.println("Fatal error in student task: " + e.getMessage());
      }
    }
    progressBar.finish();
    runner.shutdown();

    // Ensure results/ output directory exists
    File resultsDir = new File("results");
    if (!resultsDir.exists()) {
      resultsDir.mkdirs();
    }

    // 3. Report
    System.out.println("\n[3/4] Generating HTML report...");
    String[] questionKeys = getQuestionKeys(testCases);
    Reporter reporter = new Reporter(questionKeys);
    try {
      String reportPath = Path.of("results", "report.html").toString();
      reporter.writeHtmlReport(reportPath, allResults);
      System.out.println("Report written to: " + reportPath);
    } catch (IOException e) {
      System.err.println("ERROR: Failed to write report: " + e.getMessage());
    }

    // 4. Write CSV
    System.out.println("\n[4/4] Writing CSV output...");
    if (scoresheetPath != null) {
      try {
        String targetName = (outputPath == null || outputPath.trim().isEmpty())
            ? "results.csv"
            : new File(outputPath).getName();
        String resultsCsvPath = Path.of("results", targetName).toString();
        reporter.writeCsv(scoresheetPath, resultsCsvPath, allResults);
        System.out.println("Results written to: " + resultsCsvPath);
      } catch (IOException e) {
        System.err.println("ERROR: Failed to write CSV: " + e.getMessage());
      }
    }

    return true;
  }

  private String[][] buildTestCases(String configQuestions) {
    String[][] auto = discoverTestCasesFromTemplateAndTesters();
    if (auto.length > 0) {
      System.out.println("Auto-detected " + auto.length + " test case(s) from template/testers.");
      return auto;
    }
    if (configQuestions != null && !configQuestions.trim().isEmpty()) {
      System.out.println("Using config.questions because auto-detection found none.");
      return parseTestCases(configQuestions);
    }
    System.err.println("WARNING: No test cases found. Check template/testers or config.questions.");
    return new String[0][0];
  }

  private String[][] discoverTestCasesFromTemplateAndTesters() {
    Map<String, String> testerKeyByLower = new HashMap<>();
    resolvedTesters.clear();
    
    findTestersRecursive(new File(testersDir));
    
    for (String key : resolvedTesters.keySet()) {
      testerKeyByLower.put(key.toLowerCase(), key);
    }

    java.util.List<String[]> list = new ArrayList<>();
    java.util.Set<String> added = new java.util.HashSet<>();
    File templateRoot = new File(templateDir);
    File[] qFolders = templateRoot.listFiles((dir, name) -> name.matches("Q[0-9]+.*"));
    if (qFolders != null) {
      java.util.Arrays.sort(qFolders, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
      for (File folder : qFolders) {
        if (!folder.isDirectory())
          continue;
        File[] javaFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".java"));
        if (javaFiles == null)
          continue;
        java.util.Arrays.sort(javaFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File file : javaFiles) {
          String fileName = file.getName();
          String base = fileName.substring(0, fileName.length() - 5);
          String testerKey = testerKeyByLower.get(base.toLowerCase());
          if (testerKey != null) {
            String key = folder.getName() + ":" + testerKey;
            if (added.add(key)) {
              list.add(new String[] { folder.getName(), testerKey });
            }
          }
        }
      }
    }

    if (!list.isEmpty()) {
      return list.toArray(new String[0][0]);
    }

    // Fallback: derive from tester names if template matching yields nothing
    if (!resolvedTesters.isEmpty()) {
      java.util.List<String[]> fallback = new ArrayList<>();
      for (String key : resolvedTesters.keySet()) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("^Q(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(key);
        if (m.find()) {
          fallback.add(new String[] { "Q" + m.group(1), key });
        }
      }
      fallback.sort((a, b) -> {
        int folderCmp = a[0].compareToIgnoreCase(b[0]);
        return folderCmp != 0 ? folderCmp : a[1].compareToIgnoreCase(b[1]);
      });
      return fallback.toArray(new String[0][0]);
    }

    return new String[0][0];
  }

  private void findTestersRecursive(File dir) {
    if (dir == null || !dir.exists() || !dir.isDirectory())
      return;
    File[] children = dir.listFiles();
    if (children == null)
      return;
    for (File child : children) {
      if (child.isDirectory()) {
        findTestersRecursive(child);
      } else if (child.getName().endsWith("Tester.java")) {
        String name = child.getName();
        String key = name.substring(0, name.length() - "Tester.java".length());
        resolvedTesters.putIfAbsent(key, child);
      }
    }
  }

  private String[][] parseTestCases(String raw) {
    // Q1:Q1a,Q1b;Q2:Q2a;Q3:Q3
    List<String[]> list = new ArrayList<>();
    String[] sections = raw.split(";");
    for (String s : sections) {
      String[] parts = s.split(":");
      if (parts.length < 2)
        continue;
      String folder = parts[0];
      String[] qs = parts[1].split(",");
      for (String q : qs) {
        list.add(new String[] { folder, q });
      }
    }
    return list.toArray(new String[0][0]);
  }

  private String[] getQuestionKeys(String[][] testCases) {
    String[] keys = new String[testCases.length];
    for (int i = 0; i < testCases.length; i++) {
      keys[i] = testCases[i][1];
    }
    return keys;
  }

  private GradeResult processStudent(File zip, String[][] testCases, Runner runner, Grader grader) {
    String studentId = Validator.deriveStudentIdFromZip(zip.getName());
    String studentExtractDir = workDir + "/" + studentId;
    GradeResult result = new GradeResult(studentId, true);

    try {
      Validator studentValidator = new Validator(templateDir, true);
      ValidationResult vr = studentValidator.validateZipSubmission(zip.getAbsolutePath(), workDir);
      for (String a : vr.getAnomalies()) {
        result.addAnomaly("Validation: " + a);
      }
      FileUtil.unzip(zip.getAbsolutePath(), new File(studentExtractDir));

      File submissionRoot = FileUtil.findSubmissionRoot(new File(studentExtractDir));
      if (submissionRoot == null) {
        String msg = "Question folders (Q1, Q2, etc.) not found in submission.";
        System.err.println("  [" + studentId + "] SKIP: " + msg);
        result.addAnomaly("Structure: " + msg);
        return result;
      }
      // 1. Prepare Testers and execute per-question
      for (String[] tc : testCases) {
        String folder = tc[0];
        String questionKey = tc[1];
        String testerName = questionKey + "Tester";
        File testerFile = resolvedTesters.get(questionKey);

        if (testerFile == null) {
          result.setQuestionScore(questionKey, 0.0);
          result.addAnomaly("Tester Lookup: Could not find resolved file for " + testerName);
          continue;
        }

        Path submissionPath = submissionRoot.toPath().resolve(folder);
        Path testerSource = testerFile.toPath();
        Path testerTarget = submissionPath.resolve(testerName + ".java");

        try {
          Files.copy(testerSource, testerTarget, StandardCopyOption.REPLACE_EXISTING);
          // Safety check: ensure paths/keys don't contain shell-sensitive characters
          String safeFolder = folder.replace("'", "").replace("\"", "").replace(";", "").replace("&", "").replace("|",
              "");
          String safeTesterName = testerName.replace("'", "").replace("\"", "").replace(";", "").replace("&", "")
              .replace("|", "");

          String cmd = String.format("cd \"%s\" && javac *.java && java -cp . %s", safeFolder, safeTesterName);
          Runner.RunOutput runResult = runner.compileAndRun(
              submissionRoot.getAbsolutePath(), studentId, cmd);

          if (runResult.dockerError) {
            result.addAnomaly("Docker: Failed to run Docker container.");
          }
          if (runResult.timedOut) {
            result.addAnomaly("Timeout: " + questionKey);
          }

          double score = grader.parseScoreFromOutput(runResult.output);
          result.setQuestionScore(questionKey, score);
          if (score == 0.0) {
            String error = grader.identifyErrorSummary(runResult.output);
            if (error != null) {
              result.setQuestionFeedback(questionKey, error);
            }
          }
        } catch (IOException e) {
          result.setQuestionScore(questionKey, 0.0);
          result.addAnomaly("Tester Setup: Failed to copy " + testerName);
        }
      }

      // Progress is tracked by the caller's ProgressBar

    } catch (Exception e) {
      String msg = e.getMessage();
      System.err.println("  [" + studentId + "] ERROR: " + msg);
      result.addAnomaly("Pipeline Error: " + msg);
    }
    return result;
  }

}

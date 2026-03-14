package grader.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import grader.model.*;
import grader.util.*;

/**
 * Validates student ZIP submissions:
 * - Extracts zip into a working directory
 * - Checks folder structure (username folder, Q1/Q2/Q3, required java files)
 * - Checks Java file headers for name/email
 * - Reports anomalies &amp; warnings without crashing the pipeline
 */
public class Validator {

  // Dynamically loaded required question folders and their java files
  private final java.util.List<String[]> requiredStructure = new java.util.ArrayList<>();

  private static final int HEADER_LINES_TO_CHECK = 15;

  // ------------------------------------------------------------------ public

  public Validator(String path, boolean isTemplate) throws IOException {
    if (isTemplate) {
      loadFromTemplate(path);
    } else {
      loadRequirements(path);
    }
  }

  private void loadRequirements(String configPath) throws IOException {
    File configFile = new File(configPath);
    if (!configFile.exists()) {
      return; // Safe fallback
    }
    try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#"))
          continue;

        String[] parts = line.split(":", 2);
        if (parts.length < 2)
          continue;

        String folderName = parts[0].trim();
        String[] files = parts[1].split(",");

        String[] reqLine = new String[files.length + 1];
        reqLine[0] = folderName;
        for (int i = 0; i < files.length; i++) {
          reqLine[i + 1] = files[i].trim();
        }
        requiredStructure.add(reqLine);
      }
    }
  }

  private void loadFromTemplate(String templatePath) throws IOException {
    File templateDir = new File(templatePath);
    if (!templateDir.exists() || !templateDir.isDirectory()) {
      throw new IOException("Template directory not found: " + templatePath);
    }

    File[] qFolders = templateDir.listFiles((dir, name) -> name.matches("Q[0-9]+.*"));
    if (qFolders != null) {
      // Sort folders numerically if possible (Q1, Q2, Q3...)
      java.util.Arrays.sort(qFolders, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

      for (File folder : qFolders) {
        if (!folder.isDirectory())
          continue;

        File[] javaFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".java"));
        if (javaFiles == null)
          continue;

        String[] reqLine = new String[javaFiles.length + 1];
        reqLine[0] = folder.getName();
        for (int i = 0; i < javaFiles.length; i++) {
          reqLine[i + 1] = javaFiles[i].getName();
        }
        requiredStructure.add(reqLine);
      }
    }
  }

  /**
   * Derive the expected student ID from the zip filename.
   * E.g. "alice.zip" → "alice"
   */
  public static String deriveStudentIdFromZip(String zipFilename) {
    if (zipFilename == null || zipFilename.isEmpty()) {
      return "unknown";
    }
    String name = new File(zipFilename).getName();
    if (name.toLowerCase().endsWith(".zip")) {
      name = name.substring(0, name.length() - 4);
    }

    // Strip LMS prefix if present (e.g., "2023-2024-")
    name = name.replaceFirst("^\\d{4}-\\d{4}-", "");

    return name;
  }

  /**
   * Main validation entry-point for a single ZIP submission.
   *
   * @param zipPath     absolute or relative path to the student's .zip file
   * @param workDirPath directory where the zip will be extracted into a
   *                    sub-folder
   * @return a ValidationResult containing anomalies / warnings (never null)
   */
  public ValidationResult validateZipSubmission(String zipPath, String workDirPath) {
    String studentId = deriveStudentIdFromZip(zipPath);
    ValidationResult result = new ValidationResult(studentId);

    // --- Step 1: Extract ZIP ---
    File extractDir = new File(workDirPath, studentId);
    result.setExtractedRoot(extractDir.getAbsolutePath());

    if (!extractZip(zipPath, extractDir, result)) {
      // Unreadable / corrupt zip — already recorded as anomaly
      return result;
    }

    // --- Step 2: Determine the submission root (username folder) ---
    File submissionRoot = resolveSubmissionRoot(extractDir, studentId, result);
    if (submissionRoot == null) {
      // Could not locate a valid root — anomalies already recorded
      return result;
    }

    // --- Step 3: Check required question folders & files ---
    checkRequiredFiles(submissionRoot, result);

    // --- Step 4: Header heuristic (name / email in first N lines) ---
    checkHeaders(submissionRoot, result);

    // --- Step 5: Double-nested folder detection ---
    checkDoubleNesting(submissionRoot, studentId, result);

    return result;
  }

  // --------------------------------------------------------- private helpers

  private void addAnomaly(ValidationResult r, String msg) {
    r.setOk(false);
    r.getAnomalies().add(msg);
  }

  private void addWarning(ValidationResult r, String msg) {
    r.getWarnings().add(msg);
  }

  private boolean extractZip(String zipPath, File destDir, ValidationResult result) {
    try {
      FileUtil.unzip(zipPath, destDir);
      return true;
    } catch (IOException e) {
      addAnomaly(result, "Unreadable zip: " + e.getMessage());
      return false;
    }
  }

  // ----------------------------- Folder structure resolution

  /**
   * Determines the correct submission root directory.
   * Checks for: username folder, RenameToYourUsername, flat hierarchy, etc.
   *
   * @return the resolved submission root, or null if not found
   */
  private File resolveSubmissionRoot(File extractDir, String studentId, ValidationResult result) {
    File[] topLevel = extractDir.listFiles();
    if (topLevel == null || topLevel.length == 0) {
      addAnomaly(result, "Extraction produced an empty directory");
      return null;
    }

    // Collect top-level directory names
    File studentFolder = null;
    boolean hasQFoldersAtRoot = false;

    for (File f : topLevel) {
      if (!f.isDirectory())
        continue;

      String dirName = f.getName();

      if (dirName.equals(studentId)) {
        studentFolder = f;
      } else if (isQuestionFolder(dirName)) {
        hasQFoldersAtRoot = true;
      }
    }

    // Rule A: resolve in priority order
    if (studentFolder != null) {
      return studentFolder; // ideal case
    }

    if (hasQFoldersAtRoot) {
      addAnomaly(result, "Missing username folder; incorrect hierarchy "
          + "(content placed directly at zip root)");
      return extractDir; // treat extractDir as the root
    }

    addAnomaly(result, "Missing expected username folder '" + studentId + "'");
    // Try the first directory found as a best-effort fallback
    for (File f : topLevel) {
      if (f.isDirectory()) {
        addWarning(result, "Using '" + f.getName()
            + "' as fallback submission root");
        return f;
      }
    }
    return null;
  }

  // ----------------------------- Required files check

  /**
   * Checks that each required question folder and its Java source files exist.
   */
  private void checkRequiredFiles(File submissionRoot, ValidationResult result) {
    for (String[] entry : requiredStructure) {
      String folderName = entry[0];
      File folder = new File(submissionRoot, folderName);

      if (!folder.exists() || !folder.isDirectory()) {
        addAnomaly(result, "Missing " + folderName + " folder");
        // Skip checking files inside a missing folder
        continue;
      }

      // Check each required file inside this question folder
      for (int i = 1; i < entry.length; i++) {
        String fileName = entry[i];
        File javaFile = new File(folder, fileName);
        if (!javaFile.exists() || !javaFile.isFile()) {
          addAnomaly(result, "Missing " + fileName + " in " + folderName
              + File.separator);
        }
      }
    }
  }

  // ----------------------------- Header heuristic check

  /**
   * For each required .java file that exists, reads the first N lines
   * and warns if no name/email pattern is detected.
   */
  private void checkHeaders(File submissionRoot, ValidationResult result) {
    for (String[] entry : requiredStructure) {
      String folderName = entry[0];
      File folder = new File(submissionRoot, folderName);
      if (!folder.exists())
        continue;

      for (int i = 1; i < entry.length; i++) {
        String fileName = entry[i];
        File javaFile = new File(folder, fileName);
        if (!javaFile.exists() || !javaFile.isFile())
          continue;

        checkSingleFileHeader(javaFile, folderName + File.separator + fileName, result);
      }
    }
  }

  /**
   * Reads the first HEADER_LINES_TO_CHECK lines of a Java file and looks
   * for basic name/email indicators.
   */
  private void checkSingleFileHeader(File javaFile, String displayName, ValidationResult result) {
    try (BufferedReader br = new BufferedReader(new FileReader(javaFile))) {
      boolean foundNameOrEmail = false;
      for (int line = 0; line < HEADER_LINES_TO_CHECK; line++) {
        String text = br.readLine();
        if (text == null)
          break;

        String lower = text.toLowerCase();
        if (lower.contains("@")
            || lower.contains("name:")
            || lower.contains("author:")
            || lower.contains("email:")) {
          foundNameOrEmail = true;
          break;
        }
      }
      if (!foundNameOrEmail) {
        addWarning(result, "Header missing name/email in " + displayName);
      }
    } catch (IOException e) {
      addWarning(result, "Could not read header of " + displayName
          + ": " + e.getMessage());
    }
  }

  // ----------------------------- Double-nesting detection

  /**
   * Detects a common mistake where the student nests their folder inside
   * another copy: e.g. alice/alice/Q1/...
   */
  private void checkDoubleNesting(File submissionRoot, String studentId, ValidationResult result) {
    File nested = new File(submissionRoot, studentId);
    if (nested.exists() && nested.isDirectory()) {
      // Check if the nested copy also contains any expected Q folders
      boolean containsQFolders = false;
      File[] nestedFiles = nested.listFiles();
      if (nestedFiles != null) {
        for (File f : nestedFiles) {
          if (f.isDirectory() && isQuestionFolder(f.getName())) {
            containsQFolders = true;
            break;
          }
        }
      }
      if (containsQFolders) {
        addWarning(result, "Double-nested folder detected: "
            + studentId + File.separator + studentId + File.separator);
      }
    }
  }

  private boolean isQuestionFolder(String dirName) {
    for (String[] req : requiredStructure) {
      if (dirName.equals(req[0]))
        return true;
    }
    return false;
  }
}

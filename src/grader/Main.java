package grader;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import grader.core.*;
import grader.model.*;
import grader.report.*;
import grader.util.*;

/**
 * Entry point for the console auto-grading system.
 *
 * Usage:
 * java src.Main --submissions <folder> --testers <folder>
 * --scoresheet <csv> --output <csv> [--workdir <folder>]
 * [--validate-only]
 *
 * Modes:
 * --validate-only Scan for .zip files, validate each, print summary.
 * (default) Full pipeline: validate → compile → grade → report.
 */
public class Main {

  public static void main(String[] args) {
    // --- Parse CLI arguments ---
    String mode = "full";
    String submissionsDir = null;
    String testersDir = null;
    String scoresheetPath = null;
    String outputPath = null;
    String workDir = null;
    String templateDir = null;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--template":
          if (i + 1 < args.length)
            templateDir = args[++i];
          break;
        case "--validate-only":
          mode = "validate-only";
          break;
        case "--submissions":
          if (i + 1 < args.length)
            submissionsDir = args[++i];
          break;
        case "--testers":
          if (i + 1 < args.length)
            testersDir = args[++i];
          break;
        case "--scoresheet":
          if (i + 1 < args.length)
            scoresheetPath = args[++i];
          break;
        case "--output":
          if (i + 1 < args.length)
            outputPath = args[++i];
          break;
        case "--workdir":
          if (i + 1 < args.length)
            workDir = args[++i];
          break;
        default:
          if (submissionsDir == null)
            submissionsDir = args[i];
          break;
      }
    }

    if (submissionsDir == null) {
      printUsageAndExit();
    }

    // Load Configuration
    Properties config = new Properties();
    try (java.io.InputStream is = new java.io.FileInputStream("config.properties")) {
      config.load(is);
    } catch (IOException e) {
      System.err.println("WARNING: Could not load config.properties, using defaults.");
    }

    // Fallbacks from config
    if (testersDir == null)
      testersDir = config.getProperty("dir.testers", "Tester-Files");
    if (workDir == null)
      workDir = config.getProperty("dir.work", "work");
    if (scoresheetPath == null)
      scoresheetPath = config.getProperty("path.scoresheet", "scoresheet.csv");
    if (outputPath == null)
      outputPath = config.getProperty("path.output", "results.csv");
    if (templateDir == null)
      templateDir = config.getProperty("path.template", "RenameToYourUsername");

    System.out.println("=== AutoGrader ===");
    System.out.println("Submissions : " + submissionsDir);
    System.out.println("Testers     : " + testersDir);
    System.out.println("Template    : " + templateDir);
    System.out.println("Work dir    : " + workDir);
    System.out.println("Mode        : " + mode);
    System.out.println();

    if (mode.equals("validate-only")) {
      runValidateOnly(submissionsDir, workDir, templateDir);
    } else {
      GradingPipeline pipeline = new GradingPipeline(
          submissionsDir, testersDir, scoresheetPath, outputPath, workDir, templateDir, config);
      if (!pipeline.run()) {
        System.exit(1);
      }
    }
  }

  // --- Validate-only remains in Main as a utility mode ---
  private static void runValidateOnly(String submissionsDir, String workDir, String templateDir) {
    File dir = new File(submissionsDir);
    File[] zipFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip"));
    if (zipFiles == null || zipFiles.length == 0) {
      System.out.println("No .zip files found in: " + submissionsDir);
      return;
    }

    System.out.println("Scanning " + zipFiles.length + " submissions...\n");

    Validator validator;
    try {
      validator = new Validator(templateDir, true);
    } catch (IOException e) {
      System.err.println("ERROR: Could not load template from '" + templateDir + "': " + e.getMessage());
      return;
    }

    int passCount = 0;
    for (File zip : zipFiles) {
      ValidationResult vr = validator.validateZipSubmission(zip.getAbsolutePath(), workDir);
      System.out.println(vr.toString());
      if (vr.isOk()) {
        passCount++;
      }
    }

    System.out.println("=== Validation Summary ===");
    System.out.println("Total scanned: " + zipFiles.length);
    System.out.println("Passed       : " + passCount);
    System.out.println("Failed       : " + (zipFiles.length - passCount));
  }

  private static void printUsageAndExit() {
    System.err.println("Usage:");
    System.err.println(
        "  java src.Main --submissions <folder> --testers <folder> --scoresheet <csv> --output <csv> [--workdir <folder>]");
    System.err.println("  java src.Main --validate-only --submissions <folder> [--workdir <folder>]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  --validate-only   Run validation only (no compile/grade/report)");
    System.err.println("  --submissions     Path to folder containing student .zip files");
    System.err.println("  --testers         Path to folder containing tester .java files (default: Tester-Files)");
    System.err.println("  --scoresheet      Path to the IS442 ScoreSheet CSV template");
    System.err.println("  --output          Path to write the results CSV");
    System.err.println("  --workdir         Working directory for extraction (default: work)");
    System.exit(1);
  }
}

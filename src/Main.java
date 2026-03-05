import java.io.File;
import java.util.ArrayList;

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

        Validator validator = new Validator();
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

        // Step 1: Validate
        System.out.println("[1/4] Validating submissions...");
        // TODO: call Validator for each zip

        // Step 2: Compile & Execute
        System.out.println("[2/4] Compiling and executing... (not yet implemented)");
        // TODO: call Runner.compileAndRun()

        // Step 3: Grade
        System.out.println("[3/4] Grading... (not yet implemented)");
        // TODO: call Grader.grade()

        // Step 4: Report
        System.out.println("[4/4] Generating report... (not yet implemented)");
        // TODO: call Reporter.writeCsv()

        System.out.println("\nAutoGrader finished.");
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

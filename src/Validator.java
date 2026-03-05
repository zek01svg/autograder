import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    // To handle permutations like RenameToYourUsername or RenameToYourStudentID
    private static final String[] RENAMED_PLACEHOLDERS = {
            "RenameToYourUsername",
            "RenameToYourStudentID"
    };
    private static final int HEADER_LINES_TO_CHECK = 15;

    // ------------------------------------------------------------------ public

    public Validator(String configPath) throws IOException {
        loadRequirements(configPath);
    }

    private void loadRequirements(String configPath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(configPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                // Parse lines like "Q1: Q1a.java, Q1b.java"
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

    // ----------------------------- ZIP extraction

    /**
     * Extracts a ZIP file into {@code destDir}.
     * Returns false if the zip is corrupt or unreadable (anomaly added).
     */
    private boolean extractZip(String zipPath, File destDir, ValidationResult result) {
        File zipFile = new File(zipPath);
        if (!zipFile.exists() || !zipFile.isFile()) {
            addAnomaly(result, "Zip file not found: " + zipPath);
            return false;
        }

        destDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            boolean hasEntries = false;
            byte[] buffer = new byte[4096];

            while ((entry = zis.getNextEntry()) != null) {
                hasEntries = true;
                // Sanitise path to prevent zip-slip attacks
                String entryName = entry.getName().replace('\\', '/');
                File outFile = new File(destDir, entryName);

                // Security: ensure extracted path stays within destDir
                if (!outFile.getCanonicalPath().startsWith(destDir.getCanonicalPath() + File.separator)
                        && !outFile.getCanonicalPath().equals(destDir.getCanonicalPath())) {
                    addWarning(result, "Skipped suspicious zip entry: " + entryName);
                    zis.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }

            if (!hasEntries) {
                addAnomaly(result, "Zip file is empty: " + zipPath);
                return false;
            }

        } catch (IOException e) {
            addAnomaly(result, "Unreadable zip: " + e.getMessage());
            return false;
        }
        return true;
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
        File renameFolder = null;
        boolean hasQFoldersAtRoot = false;

        for (File f : topLevel) {
            if (!f.isDirectory())
                continue;

            String dirName = f.getName();

            if (dirName.equals(studentId)) {
                studentFolder = f;
            } else if (isPlaceholder(dirName)) {
                renameFolder = f;
            } else if (isQuestionFolder(dirName)) {
                hasQFoldersAtRoot = true;
            }
        }

        // Rule A: resolve in priority order
        if (studentFolder != null) {
            return studentFolder; // ideal case
        }

        if (renameFolder != null) {
            addAnomaly(result, "Folder not renamed: found '" + renameFolder.getName()
                    + "' instead of '" + studentId + "'");
            return renameFolder; // still validate contents inside
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

    private boolean isPlaceholder(String dirName) {
        for (String p : RENAMED_PLACEHOLDERS) {
            if (dirName.equals(p))
                return true;
        }
        return false;
    }
}

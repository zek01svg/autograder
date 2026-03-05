package autograder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates student submission structure and required files.
 */
public class Validator {

    private static final String[] REQUIRED_FILES = { "Main.java" };

    /**
     * Validates that a submission directory has the expected structure.
     *
     * @param submissionPath path to the student's submission directory
     * @return list of validation error messages; empty if valid
     */
    public List<String> validateStructure(String submissionPath) {
        List<String> errors = new ArrayList<>();

        // TODO: Check that submissionPath exists and is a directory
        File dir = new File(submissionPath);
        if (!dir.exists() || !dir.isDirectory()) {
            errors.add("Submission path does not exist or is not a directory: " + submissionPath);
            return errors;
        }

        // TODO: Check for required files
        for (String requiredFile : REQUIRED_FILES) {
            File f = new File(dir, requiredFile);
            if (!f.exists()) {
                errors.add("Missing required file: " + requiredFile);
            }
        }

        // TODO: Check file is not empty
        // TODO: Check file encoding is UTF-8
        // TODO: Check no disallowed imports (e.g., java.io.File for sandboxing)

        return errors;
    }

    /**
     * Checks whether a single file meets basic formatting requirements.
     *
     * @param filePath path to the file to check
     * @return true if the file passes basic checks
     */
    public boolean checkFileBasics(String filePath) {
        // TODO: Implement file size limit check
        // TODO: Implement line-length check
        // TODO: Implement encoding check
        return true; // placeholder
    }
}

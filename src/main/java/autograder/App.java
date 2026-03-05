package autograder;

/**
 * Entry point for the console auto-grading system.
 * Orchestrates validation, compilation/execution, grading, and reporting.
 */
public class App {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java autograder.App <submissions-dir>");
            System.exit(1);
        }

        String submissionsDir = args[0];
        System.out.println("AutoGrader starting...");
        System.out.println("Submissions directory: " + submissionsDir);

        // TODO: 1. Validate submission structure
        // TODO: 2. Compile and execute each submission
        // TODO: 3. Grade results against expected output
        // TODO: 4. Generate CSV report

        System.out.println("AutoGrader finished.");
    }
}

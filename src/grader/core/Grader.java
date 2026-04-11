
/**
 * Grades the output of a student submission against expected results.
 * Parses tester stdout to extract the numeric score (last line of output).
 */

package grader.core;

public class Grader {

  /**
   * Grades the output by comparing actual against expected.
   * 
   * @param studentId      The ID of the student
   * @param compiled       Whether the submission compiled successfully
   * @param actualOutput   The actual console output of the submission
   * @param expectedOutput The raw expected output
   * @return GradeResult containing score and feedback
   */
  /**
   * Parse the numeric score from the last non-empty line of tester output.
   */
  public double parseScoreFromOutput(String output) {
    if (output == null)
      return 0.0;
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

    int testCount = 0;
    int passedCount = 0;
    boolean currentTestHasResult = false;
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.matches("^Test\\s+\\d+.*:.*")) {
        testCount++;
        currentTestHasResult = false;
        continue;
      }
      if (!currentTestHasResult) {
        if ("Passed".equals(trimmed)) {
          passedCount++;
          currentTestHasResult = true;
        } else if ("Failed".equals(trimmed)) {
          currentTestHasResult = true;
        }
      }
    }
    if (passedCount > 0) {
      // Conservative fallback: only count explicit tester passes within test blocks.
      return Math.min(passedCount, testCount);
    }
    return 0.0;
  }

  /**
   * Identifies an error reason if the score is zero.
   */
  public String identifyErrorSummary(String output) {
    if (output == null || output.trim().isEmpty()) {
      return "No output produced (possible infinite loop or crash).";
    }
    if (output.contains("error: ") && output.contains("javac")) {
      return "Compilation failed (check student syntax).";
    }
    if (output.contains("error: ") || output.contains("Exception in thread")) {
      // Extract the first hint of the error
      String[] lines = output.split("\n");
      for (String line : lines) {
        if (line.contains("error:") || line.contains("Exception")) {
          return line.trim();
        }
      }
      return "Runtime exception occurred.";
    }
    if (output.contains("Could not find or load main class")) {
      return "Main class not found (compilation likely failed).";
    }
    return null;
  }
}


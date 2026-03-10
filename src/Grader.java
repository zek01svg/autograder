package src;

/**
 * Grades the output of a student submission against expected results.
 * (Stub — to be implemented on feature/compile-execute-grade branch.)
 */

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
  public GradeResult grade(String studentId, boolean compiled, String actualOutput, String expectedOutput) {
    if (!compiled) {
      return new GradeResult(studentId, false, 0, "Submission did not compile.");
    }

    if (actualOutput == null)
      actualOutput = "";
    if (expectedOutput == null)
      expectedOutput = "";

    // Normalize line endings and trim trailing whitespaces
    String normalizedActual = actualOutput.replaceAll("\\r\\n", "\n").trim();
    String normalizedExpected = expectedOutput.replaceAll("\\r\\n", "\n").trim();

    if (normalizedActual.equals(normalizedExpected)) {
      return new GradeResult(studentId, true, 100, "Perfect match.");
    } else {
      // Can be expanded to show a diff in the feedback
      return new GradeResult(studentId, true, 0,
          "Output mismatch. Expected:\n" + normalizedExpected + "\n\nActual:\n" + normalizedActual);
    }
  }
}

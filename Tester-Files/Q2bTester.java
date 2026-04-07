import java.util.*;
import java.io.*;

// A mock DataException class, as the student's pre-compiled class is not available at compile time for tester
// In a real grading environment, this would extend the actual DataException.class
class DataException extends RuntimeException {
    public DataException(String message) {
        super(message);
    }
}

public class Q2bTester extends Q2b {
    private static double score;
    private static String qn = "Q2b";

    public static void main(String[] args) {
        // Create dummy files for testing
        createTestFiles();
        grade();
        System.out.println(score);
        // Clean up dummy files
        cleanupTestFiles();
    }

    private static void createTestFiles() {
        try (PrintWriter pw = new PrintWriter("students_tester.txt")) {
            pw.println("John LEE,IS101#4.0-IS102#3.0-IS103#2.5");
            pw.println("LIM Peter,IS101#3.0-IS104#3.8");
            pw.println("LEE Teck Leong,IS101#4.0-IS102#3.0");
            pw.println("Mary CHAN,IS102#3.5-IS103#3.0");
            pw.println("Andrew TAN,IS103#2.0");
            pw.println("CHAN Wei Jun,IS101#4.0-IS103#4.0");
            pw.println("Alice WONG,IS105#3.9"); // Course IS105 only one student
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        try (PrintWriter pw = new PrintWriter("students_no_course_data_tester.txt")) {
            pw.println("John DOE,CS101#3.0");
            pw.println("Jane SMITH,CS102#3.5");
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        try (PrintWriter pw = new PrintWriter("students_empty_tester.txt")) {
            // This file is intentionally left empty
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
    }

    private static void cleanupTestFiles() {
        new File("students_tester.txt").delete();
        new File("students_no_course_data_tester.txt").delete();
        new File("students_empty_tester.txt").delete();
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;

        // Test Case 1: Clear top student for a course
        {
            try {
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, "students_tester.txt", "IS102");
                String expected = "Mary CHAN-3.5";
                String actual = getTopStudent("students_tester.txt", "IS102");
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 2: Tie for top student (first occurrence should be returned)
        {
            try {
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, "students_tester.txt", "IS101");
                String expected = "John LEE-4.0"; // John LEE and CHAN Wei Jun both have 4.0, John LEE comes first
                String actual = getTopStudent("students_tester.txt", "IS101");
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 3: Course name not found in any student's records
        {
            try {
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, "students_tester.txt", "IS999");
                String actual = getTopStudent("students_tester.txt", "IS999");
                System.out.println("Failed -> Expecting a Data Exception");
            } catch (DataException ex) {
                score += 1;
                System.out.println("Passed");
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getClass().getSimpleName());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 4: File not found
        {
            try {
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, "nosuchfile_tester.txt", "IS101");
                String actual = getTopStudent("nosuchfile_tester.txt", "IS101");
                System.out.println("Failed -> Expecting a Data Exception");
            } catch (DataException ex) {
                score += 1;
                System.out.println("Passed");
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getClass().getSimpleName());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 5: File exists, but no student data for the requested course
        {
            try {
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, "students_no_course_data_tester.txt", "IS101");
                String actual = getTopStudent("students_no_course_data_tester.txt", "IS101");
                System.out.println("Failed -> Expecting a Data Exception");
            } catch (DataException ex) {
                score += 1;
                System.out.println("Passed");
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getClass().getSimpleName());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 6: Course with only one student taking it
        {
            try {
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, "students_tester.txt", "IS105");
                String expected = "Alice WONG-3.9";
                String actual = getTopStudent("students_tester.txt", "IS105");
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}

import java.util.*;
import java.io.*;

public class Q2aTester extends Q2a {
    private static double score;
    private static String qn = "Q2a";

    public static void main(String[] args) {
        // Create dummy files for testing
        createTestFiles();
        grade();
        System.out.println(score);
        // Clean up dummy files
        cleanupTestFiles();
    }

    private static void createTestFiles() {
        try (PrintWriter pw = new PrintWriter("persons_tester.txt")) {
            pw.println("John LEE-28");
            pw.println("LIM Peter-40");
            pw.println("LEE Teck Leong-44");
            pw.println("Mary CHAN-30");
            pw.println("Andrew TAN-50");
            pw.println("CHAN Wei Jun-24");
            pw.println("TEO Wong Whee-30");
            pw.println("Jason WONG-25");
            pw.println("SAM Lee-35"); // Another Lee
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        try (PrintWriter pw = new PrintWriter("persons_no_match_tester.txt")) {
            pw.println("John DOE-30");
            pw.println("Jane SMITH-25");
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        try (PrintWriter pw = new PrintWriter("persons_empty_tester.txt")) {
            // This file is intentionally left empty
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
    }

    private static void cleanupTestFiles() {
        new File("persons_tester.txt").delete();
        new File("persons_no_match_tester.txt").delete();
        new File("persons_empty_tester.txt").delete();
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;

        // Test Case 1: Multiple matches for a surname (case-insensitive) - e.g., LEE
        {
            try {
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, "persons_tester.txt", "LEE");
                double expected = (28.0 + 44.0 + 35.0) / 3.0; // John LEE (28), LEE Teck Leong (44), SAM Lee (35)
                double actual = getAverageAge("persons_tester.txt", "LEE");
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) {
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

        // Test Case 2: Surname at beginning and end, case-insensitive match (e.g., Chan)
        {
            try {
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, "persons_tester.txt", "Chan");
                double expected = (30.0 + 24.0) / 2.0; // Mary CHAN (30), CHAN Wei Jun (24)
                double actual = getAverageAge("persons_tester.txt", "Chan");
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) {
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

        // Test Case 3: Surname not found in the file
        {
            try {
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, "persons_tester.txt", "NOEXIST");
                double expected = 0.0;
                double actual = getAverageAge("persons_tester.txt", "NOEXIST");
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) {
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

        // Test Case 4: File not found
        {
            try {
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, "nosuchfile_tester.txt", "LEE");
                double expected = -1.0;
                double actual = getAverageAge("nosuchfile_tester.txt", "LEE");
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) {
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

        // Test Case 5: Empty file
        {
            try {
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, "persons_empty_tester.txt", "ANY");
                double expected = 0.0;
                double actual = getAverageAge("persons_empty_tester.txt", "ANY");
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) {
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

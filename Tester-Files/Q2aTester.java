import java.util.*;
import java.io.*;

public class Q2aTester extends Q2a {
    private static double score;
    private static String qn = "Q2a";

    public static void main(String[] args) {
        // Create personstester.txt for testing. This file contains custom data for these test cases.
        createPersonsTesterFile("personstester.txt");

        grade();
        System.out.println(score);
    }

    // Helper method to create a personstester.txt file with custom test data.
    private static void createPersonsTesterFile(String filename) {
        try (PrintWriter pw = new PrintWriter(filename)) {
            pw.println("John LEE-28");
            pw.println("LIM Peter-40");
            pw.println("LEE Teck Leong-44");
            pw.println("Mary CHAN-30");
            pw.println("Andrew TAN-50");
            pw.println("CHAN Wei Jun-24");
            pw.println("TEO Wong Whee-30");
            pw.println("Jason WONG-25");
            pw.println("Alice Smith-20"); // Surname Smith
            pw.println("Bob SMITH-30"); // Surname SMITH
            pw.println("Charlie BROWN-20"); // Surname BROWN
            pw.println("David LEE-32"); // Additional LEE for multiple matches
        } catch (FileNotFoundException e) {
            System.err.println("Error creating test file: " + filename + " - " + e.getMessage());
        }
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;
        score = 0; 

        // Test Case 1: Existing surname with multiple matches (e.g., "LEE")
        // Expects average of ages for "John LEE" (28), "LEE Teck Leong" (44), and "David LEE" (32).
        {
            try {
                String filename = "personstester.txt";
                String surname = "LEE";
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, filename, surname);
                double expected = (28.0 + 44.0 + 32.0) / 3.0; // Expected: 34.666...
                double actual = getAverageAge(filename, surname);
                System.out.printf("Expected  :|%.3f|%n", expected);
                System.out.printf("Actual    :|%.3f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) { // Using a small epsilon for double comparison
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 2: Existing surname with a single match (e.g., "WONG")
        // Expects the age of "Jason WONG" (25).
        {
            try {
                String filename = "personstester.txt";
                String surname = "WONG";
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, filename, surname);
                double expected = 25.0; 
                double actual = getAverageAge(filename, surname);
                System.out.printf("Expected  :|%.3f|%n", expected);
                System.out.printf("Actual    :|%.3f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 3: Existing surname, checking case-insensitivity and handling surnames as first/last words (e.g., "smith")
        // Expects average of ages for "Alice Smith" (20) and "Bob SMITH" (30).
        {
            try {
                String filename = "personstester.txt";
                String surname = "smith"; // Should match "Smith" (last name) and "SMITH" (last name)
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, filename, surname);
                double expected = (20.0 + 30.0) / 2.0; 
                double actual = getAverageAge(filename, surname);
                System.out.printf("Expected  :|%.3f|%n", expected);
                System.out.printf("Actual    :|%.3f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 4: Non-existent surname in the file.
        // Expects 0.0 as per problem specification.
        {
            try {
                String filename = "personstester.txt";
                String surname = "GOH";
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, filename, surname);
                double expected = 0.0;
                double actual = getAverageAge(filename, surname);
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 5: Non-existent file.
        // Expects -1.0 as per problem specification.
        {
            try {
                String filename = "nonexistenttester.txt"; // A file that definitely won't exist
                String surname = "ANY";
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, filename, surname);
                double expected = -1.0;
                double actual = getAverageAge(filename, surname);
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", actual);
                if (Math.abs(expected - actual) < 0.001) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}

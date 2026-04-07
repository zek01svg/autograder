import java.util.*;
import java.io.*;

// Assuming DataException is a RuntimeException or similar class provided as pre-compiled.
public class Q2bTester extends Q2b {
    private static double score;
    private static String qn = "Q2b";

    public static void main(String[] args) {
        // Create studentstester.txt for testing. This file contains custom data for these test cases.
        createStudentsTesterFile("studentstester.txt");

        grade();
        System.out.println(score);
    }

    // Helper method to create a studentstester.txt file with custom test data.
    private static void createStudentsTesterFile(String filename) {
        try (PrintWriter pw = new PrintWriter(filename)) {
            pw.println("John LEE,IS101#4.0-IS102#3.0-IS103#2.5");
            pw.println("LIM Peter,IS101#3.0-IS102#2.0");
            pw.println("LEE Teck Leong,IS101#4.0-IS102#3.0-IS104#3.5");
            pw.println("Mary CHAN,IS102#3.5-IS103#3.0");
            pw.println("Andrew TAN,IS103#2.0-IS104#1.0");
            pw.println("CHAN Wei Jun,IS101#3.5-IS103#4.0-IS104#3.5");
            pw.println("Bob SMITH,IS101#4.0-IS102#3.0-IS104#4.0"); // Bob has top IS101 (tied), top IS104
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

        // Test Case 1: Existing course, with a clear top student (IS103)
        // CHAN Wei Jun has 4.0, which is higher than others for IS103.
        {
            try {
                String filename = "studentstester.txt";
                String courseName = "IS103";
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, filename, courseName);
                String expected = "CHAN Wei Jun-4.0"; 
                String actual = getTopStudent(filename, courseName);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
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

        // Test Case 2: Existing course with a tie for the top GPA (IS101).
        // "John LEE", "LEE Teck Leong", and "Bob SMITH" all have 4.0 for IS101. John LEE is the first occurrence.
        {
            try {
                String filename = "studentstester.txt";
                String courseName = "IS101";
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, filename, courseName);
                String expected = "John LEE-4.0";
                String actual = getTopStudent(filename, courseName);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
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

        // Test Case 3: Course where one student is clearly the top, even with others taking the course (IS104).
        // Bob SMITH has 4.0 for IS104, higher than LEE Teck Leong (3.5) and CHAN Wei Jun (3.5) and Andrew TAN (1.0).
        {
            try {
                String filename = "studentstester.txt";
                String courseName = "IS104";
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, filename, courseName);
                String expected = "Bob SMITH-4.0";
                String actual = getTopStudent(filename, courseName);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
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

        // Test Case 4: Non-existent course in the file.
        // Expects DataException as per problem specification.
        {
            try {
                String filename = "studentstester.txt";
                String courseName = "IS999"; // Non-existent course
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, filename, courseName);
                String actual = getTopStudent(filename, courseName);
                System.out.println("Failed -> Expecting a Data Exception");
            } catch (DataException ex) {
                score += 1;
                System.out.println("Passed");
            } catch (Exception e) {
                System.out.println("Failed -> Wrong Exception: " + e.getClass().getSimpleName());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 5: Non-existent file.
        // Expects DataException as per problem specification.
        {
            try {
                String filename = "nonexistenttester.txt"; // Non-existent file
                String courseName = "IS101";
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, filename, courseName);
                String actual = getTopStudent(filename, courseName);
                System.out.println("Failed -> Expecting a Data Exception");
            } catch (DataException ex) {
                score += 1;
                System.out.println("Passed");
            } catch (Exception e) {
                System.out.println("Failed -> Wrong Exception: " + e.getClass().getSimpleName());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}

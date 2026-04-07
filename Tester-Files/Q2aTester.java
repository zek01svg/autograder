import java.util.*;
import java.io.*;

public class Q2aTester extends Q2a {
    private static double score;
    private static String qn = "Q2a";

    public static void main(String[] args) {
        prepareFile();
        grade();
        System.out.println(score);
    }

    private static void prepareFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter("personstester.txt"))) {
            out.println("John LEE-28");
            out.println("LIM Peter-40");
            out.println("LEE Teck Leong-44");
            out.println("Mary CHAN-30");
            out.println("CHAN Wei Jun-24");
            out.println("Jason WONG-25");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;

        // Test 1: Multiple matches (LEE)
        {
            try {
                String filename = "personstester.txt";
                String surname = "LEE";
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, filename, surname);
                double expected = 36.0;
                double result = getAverageAge(filename, surname);
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", result);
                if (expected == result) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test 2: Case-insensitive match (chan)
        {
            try {
                String filename = "personstester.txt";
                String surname = "chan";
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, filename, surname);
                double expected = 27.0;
                double result = getAverageAge(filename, surname);
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", result);
                if (expected == result) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test 3: No matches
        {
            try {
                String filename = "personstester.txt";
                String surname = "TAN";
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, filename, surname);
                double expected = 0.0;
                double result = getAverageAge(filename, surname);
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", result);
                if (expected == result) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test 4: File not found
        {
            try {
                String filename = "missing.txt";
                String surname = "LEE";
                System.out.printf("Test %d: getAverageAge(%s, %s)%n", tcNum++, filename, surname);
                double expected = -1.0;
                double result = getAverageAge(filename, surname);
                System.out.printf("Expected  :|%.1f|%n", expected);
                System.out.printf("Actual    :|%.1f|%n", result);
                if (expected == result) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}
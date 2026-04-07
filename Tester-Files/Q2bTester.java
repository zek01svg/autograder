import java.util.*;
import java.io.*;

public class Q2bTester extends Q2b {
    private static double score;
    private static String qn = "Q2b";

    public static void main(String[] args) {
        prepareFile();
        grade();
        System.out.println(score);
    }

    private static void prepareFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter("studentstester.txt"))) {
            out.println("John LEE,IS101#4.0-IS102#3.0-IS103#2.5");
            out.println("LIM Peter,IS101#3.0");
            out.println("LEE Teck Leong,IS101#4.0-IS102#3.0");
            out.println("Mary CHAN,IS102#3.5-IS103#3.0");
            out.println("Andrew TAN,IS103#2.0");
            out.println("CHAN Wei Jun,IS101#4.0-IS103#4.0");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;

        // Test 1: Valid course and top student (IS101)
        {
            try {
                String filename = "studentstester.txt";
                String course = "IS101";
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, filename, course);
                String expected = "John LEE-4.0";
                String result = getTopStudent(filename, course);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result)) {
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

        // Test 2: Valid course (IS103)
        {
            try {
                String filename = "studentstester.txt";
                String course = "IS103";
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, filename, course);
                String expected = "CHAN Wei Jun-4.0";
                String result = getTopStudent(filename, course);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result)) {
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

        // Test 3: Course not found -> DataException
        {
            try {
                String filename = "studentstester.txt";
                String course = "IS104";
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, filename, course);
                String result = getTopStudent(filename, course);
                System.out.println("Failed -> Expecting a Data Exception");
            } catch (DataException ex) {
                score += 1;
                System.out.println("Passed");
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test 4: File not found -> DataException
        {
            try {
                String filename = "notfound.txt";
                String course = "IS101";
                System.out.printf("Test %d: getTopStudent(%s, %s)%n", tcNum++, filename, course);
                String result = getTopStudent(filename, course);
                System.out.println("Failed -> Expecting a Data Exception");
            } catch (DataException ex) {
                score += 1;
                System.out.println("Passed");
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}
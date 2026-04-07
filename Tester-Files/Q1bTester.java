import java.util.*;

public class Q1bTester extends Q1b {
    private static double score;
    private static String qn = "Q1b";

    public static void main(String[] args) {
        grade();
        System.out.println(score);
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;

        // Test 1: All Integers, even and odd
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 12;
                int result = getSumOfEvenIntegers(inputs);
                System.out.printf("Expected  :|%d|%n", expected);
                System.out.printf("Actual    :|%d|%n", result);
                if (expected == result) {
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

        // Test 2: Mixed types (Doubles should be ignored)
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(10, 20.0, 30, "40", true));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 40;
                int result = getSumOfEvenIntegers(inputs);
                System.out.printf("Expected  :|%d|%n", expected);
                System.out.printf("Actual    :|%d|%n", result);
                if (expected == result) {
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

        // Test 3: No even integers
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(1, 3, 5, "2", 4.0));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 0;
                int result = getSumOfEvenIntegers(inputs);
                System.out.printf("Expected  :|%d|%n", expected);
                System.out.printf("Actual    :|%d|%n", result);
                if (expected == result) {
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

        // Test 4: Empty list
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>();
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 0;
                int result = getSumOfEvenIntegers(inputs);
                System.out.printf("Expected  :|%d|%n", expected);
                System.out.printf("Actual    :|%d|%n", result);
                if (expected == result) {
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
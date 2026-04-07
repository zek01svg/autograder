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

        // Test Case 1: Mixed integers (even and odd) and other objects
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(10, 5, "hello", true, 20.0, 3, 4));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 10 + 4; // 10 and 4 are even integers. 20.0 (Double) should be treated as non-integer
                int result = getSumOfEvenIntegers(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
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

        // Test Case 2: Only even integers
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(2, 4, 6, 8));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 2 + 4 + 6 + 8;
                int result = getSumOfEvenIntegers(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
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

        // Test Case 3: No even integers, only odd integers and other objects
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(1, 3, 5, "word", 7.5, false));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 0;
                int result = getSumOfEvenIntegers(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
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

        // Test Case 4: Includes zero and negative even integers
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(0, -2, 1, "test", -4, 5.0, 6));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 0 + (-2) + (-4) + 6;
                int result = getSumOfEvenIntegers(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
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

        // Test Case 5: Large numbers
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(1000000, 2000000, 3000001, "large"));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 1000000 + 2000000;
                int result = getSumOfEvenIntegers(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
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

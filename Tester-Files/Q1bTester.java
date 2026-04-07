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
        score = 0; 

        // Test Case 1: List containing only even integers.
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(10, 20, 30, 4));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 64; // 10 + 20 + 30 + 4 = 64
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 2: Mix of even, odd integers, and other data types (Double, String, Boolean).
        // Only integer `10`, `4`, `-6` are even. `20.0` is a Double, not an Integer.
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(10, "a", 3, true, 20.0, 4, -6, 5));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 8; // 10 + 4 + (-6) = 8
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 3: List containing no even integers (only odd integers or non-integer types).
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(1, 3, 5, "hello", false, 7.0));
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 4: List with Doubles that represent even numbers (should be ignored as they are not Integers).
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(2, 4.0, 6, 8.0, 10));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = 18; // Only 2, 6, 10 are Integers
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 5: List with negative even integers, including zero.
        {
            try {
                ArrayList<Object> inputs = new ArrayList<>(Arrays.asList(-2, -4, -1, 0, "test", -6.0));
                System.out.printf("Test %d: getSumOfEvenIntegers(%s)%n", tcNum++, inputs);
                int expected = -6; // -2 + -4 + 0 = -6
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}

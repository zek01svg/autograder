import java.util.*;

public class Q1aTester extends Q1a {
    private static double score;
    private static String qn = "Q1a";

    public static void main(String[] args) {
        grade();
        System.out.println(score);
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;

        // Test 1: Simple isograms
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("cat", "dog", "tiger"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                String expected = "[cat, dog, tiger]";
                ArrayList<String> result = getIsogramWords(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result.toString())) {
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

        // Test 2: Words with repeated lowercase letters
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("apple", "banana", "cherry", "dog"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                String expected = "[dog]";
                ArrayList<String> result = getIsogramWords(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result.toString())) {
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

        // Test 3: Case-insensitive repeat check
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("Alan", "Ben", "Evelyne"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                String expected = "[Ben]";
                ArrayList<String> result = getIsogramWords(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result.toString())) {
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
                ArrayList<String> inputs = new ArrayList<>();
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                String expected = "[]";
                ArrayList<String> result = getIsogramWords(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result.toString())) {
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
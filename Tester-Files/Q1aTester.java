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

        // Test Case 1: All words are isograms
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("cat", "dog", "tiger", "unique"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("cat", "dog", "tiger", "unique"));
                ArrayList<String> result = getIsogramWords(inputs);
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
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 2: Mixed words, including non-isograms with duplicates
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("apple", "banana", "orange", "grape"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("orange", "grape"));
                ArrayList<String> result = getIsogramWords(inputs);
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
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 3: Words with mixed cases, requiring case-insensitive check
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("Alabama", "Hello", "World", "Java"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("World", "Java"));
                ArrayList<String> result = getIsogramWords(inputs);
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
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 4: Words with only one or two characters
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("a", "bb", "cd", "efg"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("a", "cd", "efg"));
                ArrayList<String> result = getIsogramWords(inputs);
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
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 5: All words are non-isograms
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("repeat", "level", "mammal"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>();
                ArrayList<String> result = getIsogramWords(inputs);
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
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}

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
        score = 0; 

        // Test Case 1: All words are isograms (all lowercase, unique characters)
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("cat", "dog", "tiger"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("cat", "dog", "tiger"));
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 2: Mix of isograms and non-isograms, including mixed case letters, where duplicates are case-insensitive.
        // "level" (l, e repeated), "programming" (g, r repeated), "apple" (p repeated) are not isograms.
        // "java", "unique", "banana" are isograms (case-insensitive check).
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("level", "java", "unique", "programming", "apple", "banana"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("java", "unique", "banana")); 
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 3: No isograms in the list (all words have repeated characters).
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("bookkeeper", "Mississippi", "banana", "apple"));
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 4: Words with mixed case letters, checking case-insensitive uniqueness.
        // "HeLlo" (l repeated), "ExamPle" (e repeated) are not isograms.
        // "WorLd", "TeSt" are isograms.
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("HeLlo", "WorLd", "TeSt", "ExamPle"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("WorLd", "TeSt")); 
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 5: List containing single-character words (all are by definition isograms).
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("a", "b", "c", "D"));
                System.out.printf("Test %d: getIsogramWords(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("a", "b", "c", "D"));
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
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}

package tests;

public class TestUtils {
  public static void assertTrue(String message, boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + message);
      System.exit(1);
    } else {
      System.out.println("PASS: " + message);
    }
  }

  public static void assertEquals(String message, Object expected, Object actual) {
    if (expected == null && actual == null) {
      System.out.println("PASS: " + message);
      return;
    }
    if (expected != null && expected.equals(actual)) {
      System.out.println("PASS: " + message);
    } else {
      System.err.println("FAIL: " + message + " (Expected: [" + expected + "], Actual: [" + actual + "])");
      System.exit(1);
    }
  }
}

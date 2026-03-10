package tests;

import src.Grader;
import src.GradeResult;

public class GraderTest {
  public static void main(String[] args) {
    Grader grader = new Grader();

    // 1. Test "Perfect match"
    GradeResult res1 = grader.grade("stu1", true, "Hello World\n", "Hello World");
    TestUtils.assertEquals("stu1: score should be 100 on perfect match", 100, res1.getScore());
    TestUtils.assertTrue("stu1: should be marked as compiled", res1.isCompiled());

    // 2. Test "Mismatch"
    GradeResult res2 = grader.grade("stu2", true, "Something else", "Expected output");
    TestUtils.assertEquals("stu2: score should be 0 on mismatch", 0, res2.getScore());

    // 3. Test "Not compiled"
    GradeResult res3 = grader.grade("stu3", false, "", "Expected output");
    TestUtils.assertEquals("stu3: score should be 0 when not compiled", 0, res3.getScore());
    TestUtils.assertTrue("stu3: should be marked as NOT compiled", !res3.isCompiled());

    System.out.println("GraderTest: All unit tests passed.");
  }
}

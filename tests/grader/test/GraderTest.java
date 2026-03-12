package grader.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import grader.core.*;
import grader.model.*;
import grader.report.*;
import grader.util.*;

public class GraderTest {
  public static void main(String[] args) {
    Grader grader = new Grader();

    // 1. Test "Perfect match" (Tester prints score as last line)
    double score1 = grader.parseScoreFromOutput("Running tests...\nPassed 3/3\n3.0");
    TestUtils.assertEquals("stu1: score should be 3.0", 3.0, score1);

    // 2. Test "Partial match"
    double score2 = grader.parseScoreFromOutput("Running tests...\nPassed 1/3\n1.0");
    TestUtils.assertEquals("stu2: score should be 1.0", 1.0, score2);

    // 3. Test "Empty output"
    double score3 = grader.parseScoreFromOutput("");
    TestUtils.assertEquals("stu3: score should be 0.0", 0.0, score3);

    // 4. Test "Missing final score line" (fallback to Passed count)
    double score4 = grader.parseScoreFromOutput("Test 1\nPassed\nTest 2\nFailed\n");
    TestUtils.assertEquals("stu4: score should be 1.0", 1.0, score4);

    System.out.println("GraderTest: All unit tests passed.");
  }
}


import { chat } from "@tanstack/ai";
import * as Gemini from "@tanstack/ai-gemini";
import { TestFilesOutputSchema, TestFilesOutput } from "./schema";
import { normalizeChatError } from "./error-utils";

const DEFAULT_MODEL_NAME = "gemini-3-flash-preview";

export async function generateTestFiles({
  questionPaper,
  templateStructure,
  apiKey,
}: {
  questionPaper: string;
  templateStructure: string;
  apiKey: string;
}): Promise<TestFilesOutput> {
  const systemPrompt = `You are an expert Java Test Engineer for an OOP course auto-grading system.
Generate Java tester files based on the Question Paper and Student Template code below.

STUDENT TEMPLATE (with source code):
${templateStructure}

QUESTION PAPER:
${questionPaper}

### TESTER FILE CONVENTION (MUST FOLLOW EXACTLY)
The auto-grader parses stdout to extract scores. You MUST use this exact format:
1. **Filename**: \`{QuestionId}Tester.java\` (e.g., Q1aTester.java, Q2bTester.java, Q3Tester.java)
2. **No package declaration**. Import \`java.util.*\` at the top.
3. **Class extends the student's class**: e.g., \`public class Q1aTester extends Q1a {}\`
4. **Fields**: \`private static double score;\` and \`private static String qn = "Q1a";\`
5. **main()**: Call grade(), then print score: \`System.out.println(score);\` — MUST be the last stdout line.
6. **grade()**: Contains all test cases. NO JUnit — use plain Java with try-catch.
7. **Stdout format per test case**:
   - \`System.out.printf("Test %d: methodName(%s)%n", tcNum++, inputs);\`
   - \`System.out.printf("Expected  :|%s|%n", expected);\`
   - \`System.out.printf("Actual    :|%s|%n", result);\`
   - Print "Passed" or "Failed"
   - On pass: \`score += 1;\`

### EXAMPLE TESTER:
\`\`\`java
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
        {
            try {
                ArrayList<String> inputs = new ArrayList<>();
                inputs.add("alice");
                System.out.printf("Test %d: methodName(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("alice"));
                ArrayList<String> result = methodName(inputs);
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
\`\`\`

### FOR EXCEPTION TESTING (e.g., DataException):
\`\`\`java
try {
    String result = someMethod(args);
    System.out.println("Failed -> Expecting a Data Exception");
} catch (DataException ex) {
    System.out.println("Passed");
    score += 1;
} catch (Exception e) {
    System.out.println("Failed -> Exception");
    e.printStackTrace();
}
\`\`\`

### RULES:
- Generate ONE tester file per sub-question (Q1a, Q1b, Q2a, Q2b, Q3, etc.)
- Analyze the student's Java source code to find method signatures, parameter types, return types
- The tester extends the student's class, so call methods DIRECTLY (no object instantiation needed)
- Include 3-5 test cases per tester covering normal, edge, and boundary cases
- SCORING: Each test case is worth EXACTLY 1 point. ALWAYS use \`score += 1;\` — NEVER use fractional values like 0.5
- Use realistic test data derived from the question paper requirements
- If the question references data files (.txt), use filenames from the template but with "tester" suffix (e.g., "personstester.txt")
- CODE MUST use real newlines and 4-space indentation — NO minification
`;

  const adapter = Gemini.createGeminiChat(DEFAULT_MODEL_NAME, apiKey);
  try {
    const response = await chat({
      adapter,
      systemPrompts: [systemPrompt],
      messages: [
        {
          role: "user",
          content: "Generate the test files for this assignment based on the provided context.",
        },
      ],
      outputSchema: TestFilesOutputSchema,
    });

    const files = response as TestFilesOutput;

    // Safety post-processing: expand minified code to ensure vertical layout
    const formattedFiles = files.map((file) => ({
      ...file,
      code:
        file.code.includes("\n") && file.code.split("\n").length > 5
          ? file.code
          : file.code
              .replace(/package\s+([\w.]+);/g, "package $1;\n\n")
              .replace(/import\s+([\w.*]+);/g, "import $1;\n")
              .replace(/public\s+class/g, "\npublic class")
              .replace(/\{/g, " {\n")
              .replace(/\}/g, "\n}\n")
              .replace(/;/g, ";\n")
              .replace(/\n\s*\n/g, "\n\n")
              .trim(),
    }));

    return formattedFiles as TestFilesOutput;
  } catch (error: unknown) {
    const normalized = normalizeChatError(error);
    console.error("AI Generation Error Normalized:", normalized);

    // Throw a specialized error object that the API route can handle
    const enhancedError = new Error(normalized.message);
    (enhancedError as any).status = normalized.status;
    (enhancedError as any).retryAfterSeconds = normalized.retryAfterSeconds;
    throw enhancedError;
  }
}

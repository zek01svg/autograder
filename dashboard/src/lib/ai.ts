import { Ollama } from "ollama";
import { z } from "zod";
import { zodToJsonSchema } from "zod-to-json-schema";
import { TestFilesOutputSchema } from "./schema";
import logger from "./pino";

const DEFAULT_MODEL_NAME = "qwen2.5-coder:7b";

const customFetch = (input: RequestInfo | URL, init?: RequestInit) => {
  return fetch(input, {
    ...init,
    // @ts-ignore
    headersTimeout: 900000,
    bodyTimeout: 900000,
    connectTimeout: 60000,
  });
};

const ollama = new Ollama({ 
  host: "http://localhost:11434",
  fetch: customFetch as any,
});

export async function generateTestFiles({
  questionPaper,
  templateStructure,
}: {
  questionPaper: string;
  templateStructure: string;
}): Promise<ReadableStream<any>> {
  logger.info({
    msg: "Starting generation with Structured Outputs",
    model: DEFAULT_MODEL_NAME
  });

  // Prompt adapted from the battle-tested Gemini dev branch implementation
  const systemPrompt = `You are an expert Java Test Engineer for an OOP course auto-grading system.
Generate Java tester files based on the Question Paper and Student Template code below.

STUDENT TEMPLATE (with source code):
${templateStructure}

QUESTION PAPER:
${questionPaper}

### TESTER FILE CONVENTION (MUST FOLLOW EXACTLY)
The auto-grader parses stdout to extract scores. You MUST use this exact format:
1. **Filename**: \`{QuestionId}Tester.java\` (e.g., Q1aTester.java, Q2bTester.java, Q3Tester.java). FLAT filename only — NO folder prefix like "RenameToYourUsername/Q1/".
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

### EXAMPLE TESTER (showing 4 test cases — this is the MINIMUM pattern to follow):
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
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("alice", "ben"));
                System.out.printf("Test %d: methodName(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("alice", "ben"));
                ArrayList<String> result = methodName(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result)) { score += 1; System.out.println("Passed"); }
                else { System.out.println("Failed"); }
            } catch (Exception e) { System.out.println("Failed -> Exception"); e.printStackTrace(); }
            System.out.println("-------------------------------------------------------");
        }
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("ellen", "alan"));
                System.out.printf("Test %d: methodName(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("alan"));
                ArrayList<String> result = methodName(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result)) { score += 1; System.out.println("Passed"); }
                else { System.out.println("Failed"); }
            } catch (Exception e) { System.out.println("Failed -> Exception"); e.printStackTrace(); }
            System.out.println("-------------------------------------------------------");
        }
        {
            try {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList("Elle", "Daniel"));
                System.out.printf("Test %d: methodName(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>(Arrays.asList("Daniel"));
                ArrayList<String> result = methodName(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result)) { score += 1; System.out.println("Passed"); }
                else { System.out.println("Failed"); }
            } catch (Exception e) { System.out.println("Failed -> Exception"); e.printStackTrace(); }
            System.out.println("-------------------------------------------------------");
        }
        {
            try {
                ArrayList<String> inputs = new ArrayList<>();
                System.out.printf("Test %d: methodName(%s)%n", tcNum++, inputs);
                ArrayList<String> expected = new ArrayList<>();
                ArrayList<String> result = methodName(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result)) { score += 1; System.out.println("Passed"); }
                else { System.out.println("Failed"); }
            } catch (Exception e) { System.out.println("Failed -> Exception"); e.printStackTrace(); }
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
- TOTAL SCORE CONSTRAINT: Read the marks allocated per question from the question paper. The number of test cases per file MUST match the marks for that question (e.g., if Q1a is worth 3 marks, generate exactly 3 test cases). Each test case = 1 point.
- SCORING: Each test case is worth EXACTLY 1 point. ALWAYS use \`score += 1;\` — NEVER use fractional values
- Use realistic test data derived from the question paper requirements
- Test normal cases, edge cases (empty input, single element), and boundary cases
- If the question references data files (.txt), use filenames from the template but with "tester" suffix (e.g., "personstester.txt")
- For primitive return types (int, double, boolean), use == for comparison. For objects (String, ArrayList), use .equals()
- CODE MUST use real newlines and 4-space indentation — NO minification
`;

  try {
    const stream = await ollama.chat({
      model: DEFAULT_MODEL_NAME,
      messages: [
        { role: "system", content: systemPrompt },
        { 
          role: "user", 
          content: `Generate the test files for this assignment. IMPORTANT: Each tester file MUST have 4 to 5 different test cases with varied inputs (normal, edge, boundary cases). Keep 'thinking' to 1 sentence. Return JSON with 'thinking' (string) and 'files' (array of {filename, code}). Filenames must be flat like Q1aTester.java.`
        },
      ],
      format: {
        type: "object",
        properties: {
          thinking: { type: "string" },
          files: {
            type: "array",
            items: {
              type: "object",
              properties: {
                filename: { type: "string" },
                code: { type: "string" }
              },
              required: ["filename", "code"]
            }
          }
        },
        required: ["thinking", "files"]
      } as any,
      stream: true,
      options: {
        num_ctx: 32768,
        num_predict: 32768,
        temperature: 0,
      }
    });

    return new ReadableStream({
      async start(controller) {
        const encoder = new TextEncoder();
        let fullContent = "";
        try {
          for await (const chunk of stream) {
            fullContent += chunk.message.content;
            const data = JSON.stringify({
              type: "content",
              delta: chunk.message.content,
            });
            controller.enqueue(encoder.encode(`data: ${data}\n\n`));
          }

          // Post-process: validate and fix the generated files
          try {
            const parsed = JSON.parse(fullContent);
            if (parsed.files && Array.isArray(parsed.files)) {
              const fixedFiles = parsed.files.map((file: any) => {
                // 1. Ensure flat filename (strip any folder prefix)
                let filename = file.filename || "";
                const parts = filename.split("/");
                filename = parts[parts.length - 1]; // Take just the basename
                if (!filename.endsWith(".java")) {
                  filename = filename + ".java";
                }

                // 2. Expand minified code if needed
                let code = file.code || "";
                if (code.includes("\n") && code.split("\n").length > 5) {
                  // Already properly formatted
                } else {
                  // Attempt to expand minified code
                  code = code
                    .replace(/package\s+([\w.]+);/g, "package $1;\n\n")
                    .replace(/import\s+([\w.*]+);/g, "import $1;\n")
                    .replace(/public\s+class/g, "\npublic class")
                    .replace(/\{/g, " {\n")
                    .replace(/\}/g, "\n}\n")
                    .replace(/;/g, ";\n")
                    .replace(/\n\s*\n/g, "\n\n")
                    .trim();
                }

                return { ...file, filename, code };
              });

              // Send the fixed version as a final event
              const fixedData = JSON.stringify({
                type: "fixed_files",
                files: fixedFiles,
              });
              controller.enqueue(encoder.encode(`data: ${fixedData}\n\n`));
            }
          } catch (e) {
            // Post-processing failed, the raw stream is still available
            logger.warn({ msg: "Post-processing of AI output failed", error: e });
          }

          controller.close();
        } catch (error) {
          console.error("[AI Stream] Error during streaming:", error);
          controller.error(error);
        }
      },
    });
  } catch (error: unknown) {
    console.error("Ollama direct generation error:", error);
    throw error;
  }
}

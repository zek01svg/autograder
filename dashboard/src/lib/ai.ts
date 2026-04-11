import { Ollama } from "ollama";
import logger from "@/lib/pino";

const DEFAULT_MODEL_NAME = "qwen2.5-coder:3b";

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

  const systemPrompt = `You are an expert Java Test Grader.
Your goal is to generate pure Java tester files that extend student classes and provide a final score.

### 🚫 AGGRESSIVE RULE: NO JUNIT
- DO NOT use JUnit, TestNG, or any other testing framework.
- DO NOT use @Test, @Before, Assert.assertEquals, or any JUnit imports.
- ALL tests must be written in pure Java using standard if/else logic and try/catch blocks.

STUDENT TEMPLATE CONTEXT:
\${templateStructure}

### MANDATORY RULES:
1. ONLY generate the Tester classes. DO NOT re-write the student base files.
2. Filenames MUST end in 'Tester.java' (e.g. Q1aTester.java).
3. Classes MUST extend the base question (e.g. public class Q1aTester extends Q1a).
4. Logic: Use a 'public static int grade()' method to conduct tests.
5. Final Output: The main() method MUST call grade() and print ONLY the final integer score.
6. NO decorative lines (---). NO verbose logging.

### EXAMPLE STRUCTURE (Q1aTester.java):
{
  "filename": "RenameToYourUsername/Q1/Q1aTester.java",
  "code": "import java.util.*;\npublic class Q1aTester extends Q1a {\n    public static void main(String[] args) {\n        // ONLY print the numeric score on the last line\n        System.out.println(grade());\n    }\n    public static int grade() {\n        int score = 0;\n        try {\n            // Use standard Java logic - NO JUnit assertions\n            if (getIsogramWords(new ArrayList<>(Arrays.asList(\"cat\"))).size() == 1) score += 10;\n            if (getIsogramWords(new ArrayList<>(Arrays.asList(\"paper\"))).isEmpty()) score += 10;\n        } catch (Exception e) {\n            // Silent failure is okay for grading\n        } \n        return score;\n    }\n}"
}
`;

  try {
    const stream = await ollama.chat({
      model: DEFAULT_MODEL_NAME,
      messages: [
        { role: "system", content: systemPrompt },
        { 
          role: "user", 
          content: "Generate all requested test files (Q1aTester, Q1bTester, Q2aTester, Q2bTester, Q3Tester) as a JSON object with 'thinking' and 'files' (array of {filename, code})." 
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
        num_predict: 16384,
        temperature: 0,
      }
    });

    return new ReadableStream({
      async start(controller) {
        const encoder = new TextEncoder();
        try {
          for await (const chunk of stream) {
            const data = JSON.stringify({
              type: "content",
              delta: chunk.message.content,
            });
            controller.enqueue(encoder.encode(`data: ${data}\n\n`));
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

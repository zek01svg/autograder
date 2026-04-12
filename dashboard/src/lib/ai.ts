import { Ollama } from "ollama";
import logger from "@/lib/pino";
import { fixAiCode } from "./utils";

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

STUDENT TEMPLATE CONTEXT:
${templateStructure}

### MANDATORY RULES:
1. ONLY generate the Tester classes. DO NOT re-write base files.
2. Filenames MUST end in 'Tester.java' (e.g. Q1aTester.java).
3. Classes MUST extend the base question (e.g. public class Q1aTester extends Q1a).
4. USE THIS EXACT STRUCTURE:
   import java.util.*;
   import java.io.*;
   public class Q1aTester extends Q1a {
       private static double score = 0;
       public static void main(String[] args) {
           grade();
           System.out.println(score);
       }
       public static void grade() {
           try {
               // Use 'score += 1;' for every passed test. NO JUNIT.
           } catch (Exception e) {}
       }
   }
5. CRITICAL JAVA RULES:
   - ALWAYS include 'import java.util.*;' and 'import java.io.*;' at the top.
   - NEVER redeclare a variable type inside the same method (e.g., if 'String actual' exists, use 'actual = ...' for the next test).
   - ALL methods you call (like getIsogramWords) are STATIC as per the student templates.
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
          let fullContent = "";
          for await (const chunk of stream) {
            const content = chunk.message.content;
            fullContent += content;
            const data = JSON.stringify({
              type: "content",
              delta: content,
            });
            controller.enqueue(encoder.encode(`data: ${data}\n\n`));
          }

          // Final Post-Processing Step
          try {
            // Find the JSON block if the model included markers
            const jsonMatch = fullContent.match(/\{[\s\S]*\}/);
            if (jsonMatch) {
              const parsed = JSON.parse(jsonMatch[0]);
              if (parsed.files && Array.isArray(parsed.files)) {
                const fixedFiles = parsed.files.map((file: any) => {
                  const filename = file.filename.split('/').pop() || file.filename;
                  const code = fixAiCode(file.code || "");
                  return { ...file, filename, code };
                });
                
                const fixedData = JSON.stringify({
                  type: "fixed_files",
                  files: fixedFiles,
                });
                controller.enqueue(encoder.encode(`data: ${fixedData}\n\n`));
              }
            }
          } catch (e) {
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

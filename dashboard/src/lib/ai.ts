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
  const systemPrompt = `
    You are an expert Java Test Engineer for an Object-Oriented Programming (OOP) course.
    Your task is to generate JUnit 5 test files based on a Question Paper and a provided Student Template Structure.

    STUDENT TEMPLATE STRUCTURE:
    ${templateStructure}

    QUESTION PAPER:
    ${questionPaper}

    ### GUIDELINES:
    1. Analyze the student template structure (classes, methods, fields).
    2. Map the requirements in the question paper to specific test cases.
    3. Generate one or more JUnit 5 Java files that accurately test the student's submission.
    4. Ensure the package names match the template structure (e.g., 'package grader;').
    5. Each test should have a clear explanation.
    6. Return a JSON array of objects.
    7. CODE FORMATTING: The 'code' field MUST be formatted like a standard file with physical line breaks (\n) and proper 4-space indentation. 
    8. NO MINIFICATION: Do NOT return the code on a single line. It MUST be readable, vertical, and professional.
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

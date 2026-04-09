import { z } from "zod";

export const TestFileSchema = z.object({
  filename: z.string().describe("The name of the Java file, e.g., 'GradeTest.java'"),
  code: z
    .string()
    .describe(
      "The complete Java code for the JUnit test file. CRITICAL: Use REAL newline characters (\\n) and indent using 4 spaces. DO NOT MINIFY.",
    ),
  explanation: z.string().describe("A brief explanation of what this test case covers."),
  questionRef: z
    .string()
    .describe("The specific question number or reference from the question paper (e.g., 'Q1a')"),
});

export const TestFilesOutputSchema = z.object({
  thinking: z
    .string()
    .describe("Explain your step-by-step plan for generating these tester files."),
  files: z.array(TestFileSchema),
});

export type TestFile = z.infer<typeof TestFileSchema>;
export type TestFilesOutput = z.infer<typeof TestFilesOutputSchema>;

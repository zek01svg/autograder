import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Splits a question paper into sections based on common patterns like "Question 1", "Q2", "Task 3", "Part A".
 * Uses a positive lookahead regex to preserve the delimiters in the resulting chunks.
 */
export function splitQuestionPaper(text: string): string[] {
  if (!text) return [];

  // Look for Question [number], Q[number], Task [number], or Part [letter/number]
  const pattern = /(?=Question\s+\d+|Q\d+|Task\s+\d+|Part\s+[A-Z0-9])/gi;

  const chunks = text
    .split(pattern)
    .map((c) => c.trim())
    .filter(Boolean);

  // If no sections found, return entire text as one chunk
  if (chunks.length === 0) return [text];

  return chunks;
}

/**
 * Heuristic to find relevant files from a list based on keywords in a text block.
 * Prunes the context sent to the AI to reduce noise and token usage.
 */
export function getRelevantFiles(sectionText: string, allFilesPaths: string[]): string {
  // 1. Extract PascalCase words (potential Java Classes)
  const classMatches = sectionText.match(/[A-Z][a-zA-Z0-9]+/g) || [];
  const uniqueKeywords = new Array(...new Set(classMatches));

  // Extract potential Question markers (e.g., Q1, Q2, "Question 1", "Question 2")
  const qMatch = sectionText.match(/\bQ(\d+)\b/i);
  const questionMatch = sectionText.match(/\bQuestion\s+(\d+)\b/i);
  const qNum = qMatch ? qMatch[1] : questionMatch ? questionMatch[1] : null;
  const qMarker = qNum ? `q${qNum}` : null;

  const relevant = allFilesPaths.filter((filePath) => {
    const fileName = filePath.split("/").pop() || "";
    const baseName = fileName.replace(/\.[^/.]+$/, ""); // Remove extension

    // Always include common/exception files
    if (fileName.toLowerCase().includes("exception")) return true;

    // Explicit mention of the filename (without extension) in the text
    if (uniqueKeywords.includes(baseName)) return true;

    // Relative path matches Question marker (e.g., Q1/Q1a.java matches Q1)
    if (qMarker && filePath.toLowerCase().includes(`${qMarker}/`)) return true;

    return false;
  });

  return relevant.join("\n");
}

/**
 * Throttled Promise.all to manage concurrency and avoid rate limits.
 */
export async function batchPromiseAll<T>(
  tasks: (() => Promise<T>)[],
  limit: number,
  onProgress?: (completed: number) => void,
): Promise<T[]> {
  const results: T[] = [];

  for (let i = 0; i < tasks.length; i += limit) {
    const batch = tasks.slice(i, i + limit);
    const batchResults = await Promise.all(batch.map((task) => task()));
    results.push(...batchResults);

    if (onProgress) {
      onProgress(Math.min(i + limit, tasks.length));
    }

    // Small delay between batches to stay safe on free tier
    if (i + limit < tasks.length) {
      await new Promise((r) => setTimeout(r, 1000));
    }
  }

  return results;
}

/**
 * Post-processes AI-generated Java code to fix common syntax errors and ensure compatibility.
 */
export function fixAiCode(code: string): string {
  if (!code) return "";
  let fixed = code;

  // 1. Remove package declarations (Zero-Package rule)
  fixed = fixed.replace(/package\s+[\w.]+;/g, "");

  // 2. Auto-Inject Missing Imports
  if (!fixed.includes("import java.util.*")) {
    fixed = "import java.util.*;\nimport java.io.*;\n" + fixed;
  }

  // 3. Fix Variable Redeclarations (e.g. second 'String actual =' -> 'actual =')
  const commonVars = ["actual", "inputs", "result", "expected", "val", "data", "list"];
  for (const v of commonVars) {
    // Matches: [Type] [varName] = ... -> replaces with just [varName] = ... on subsequent matches
    const regex = new RegExp(`(\\b(?:String|int|double|boolean|List<[^>]*>|ArrayList<[^>]*>|var)\\s+)(${v}\\b)`, "g");
    let matchCount = 0;
    fixed = fixed.replace(regex, (match, type, name) => {
      matchCount++;
      return matchCount > 1 ? name : match;
    });
  }

  // 4. Normalize score increments to strictly += 1 
  fixed = fixed.replace(/score\s*\+=\s*(?!1\s*;)(\d+(?:\.\d+)?)/g, "score += 1");

  return fixed.trim();
}

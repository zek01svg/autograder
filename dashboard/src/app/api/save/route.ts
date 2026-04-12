import { NextResponse } from "next/server";
import fs from "fs";
import path from "path";
import logger from "@/lib/pino";
import { fixAiCode } from "@/lib/utils";

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { files } = body;

    logger.info({
      msg: "save request received",
      filesCount: files?.length || 0,
    });

    if (!files || !Array.isArray(files)) {
      logger.error({
        msg: "Save failed: Invalid file array received",
        body,
      });
      return NextResponse.json(
        { error: "Invalid payload: 'files' must be an array." },
        { status: 400 },
      );
    }

    // Path to Tester-Files relative to the dashboard directory
    const testerFilesDir = path.resolve(process.cwd(), "..", "Tester-Files");
    logger.info({
      msg: "Target directory for tests",
      testerFilesDir,
    });

    // 1. Stale File Cleanup: Remove all existing *Tester.java files before saving new ones
    if (fs.existsSync(testerFilesDir)) {
      const existingFiles = fs.readdirSync(testerFilesDir);
      for (const f of existingFiles) {
        if (f.endsWith("Tester.java")) {
          fs.unlinkSync(path.join(testerFilesDir, f));
        }
      }
      logger.info("Stale Tester.java files cleaned up");
    } else {
      logger.info(`Creating missing directory: ${testerFilesDir}`);
      fs.mkdirSync(testerFilesDir, { recursive: true });
    }

    // 2. Process and Save each file
    for (const file of files) {
      if (!file.filename || !file.code) {
        logger.warn("Skipping invalid file object:", file);
        continue;
      }

      // a. Filename Flattening: Strip junk prefixes like RenameToYourUsername/Q1/ 
      const flattenedName = path.basename(file.filename);
      const filePath = path.join(testerFilesDir, flattenedName);

      // b. Auto-Fix: Ensure imports exist, deduplicate vars, and normalize scores
      const normalizedCode = fixAiCode(file.code || "");

      fs.writeFileSync(filePath, normalizedCode, "utf8");
      
      logger.info({
        msg: "test file processed and saved",
        originalName: file.filename,
        flattenedName,
        filePath,
      });
    }

    return NextResponse.json({ success: true, count: files.length });
  } catch (error: unknown) {
    logger.error({
      msg: "Critical Save Error",
      error,
    });
    const message = error instanceof Error ? error.message : "Internal filesystem error";
    return NextResponse.json({ error: `Failed to save files: ${message}` }, { status: 500 });
  }
}

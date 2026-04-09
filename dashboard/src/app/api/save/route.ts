import { NextResponse } from "next/server";
import fs from "fs";
import path from "path";
import logger from "@/lib/pino";  

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

    // Ensure the directory exists
    if (!fs.existsSync(testerFilesDir)) {
      logger.info(`Creating missing directory: ${testerFilesDir}`);
      fs.mkdirSync(testerFilesDir, { recursive: true });
    }

    // Save each file
    for (const file of files) {
      if (!file.filename || !file.code) {
        logger.warn("Skipping invalid file object:", file);
        continue;
      }
      const filePath = path.join(testerFilesDir, file.filename);
      // Recursively create subdirectories if they don't exist
      fs.mkdirSync(path.dirname(filePath), { recursive: true });
      fs.writeFileSync(filePath, file.code, "utf8");
      logger.info({
        msg: "test files saved",
        filename: file.filename,
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

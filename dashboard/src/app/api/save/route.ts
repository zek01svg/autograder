import { NextResponse } from "next/server";
import fs from "fs";
import path from "path";

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { files } = body;

    console.log(`Received save request for ${files?.length || 0} files`);

    if (!files || !Array.isArray(files)) {
      console.error("Save failed: Invalid file array received", body);
      return NextResponse.json(
        { error: "Invalid payload: 'files' must be an array." },
        { status: 400 },
      );
    }

    // Path to Tester-Files relative to the dashboard directory
    const testerFilesDir = path.resolve(process.cwd(), "..", "Tester-Files");
    console.log(`Target directory for tests: ${testerFilesDir}`);

    // Ensure the directory exists
    if (!fs.existsSync(testerFilesDir)) {
      console.log(`Creating missing directory: ${testerFilesDir}`);
      fs.mkdirSync(testerFilesDir, { recursive: true });
    }

    // Save each file
    for (const file of files) {
      if (!file.filename || !file.code) {
        console.warn("Skipping invalid file object:", file);
        continue;
      }
      const filePath = path.join(testerFilesDir, file.filename);
      fs.writeFileSync(filePath, file.code, "utf8");
      console.log(`[SUCCESS] Saved: ${file.filename} -> ${filePath}`);
    }

    return NextResponse.json({ success: true, count: files.length });
  } catch (error: unknown) {
    console.error("Critical Save Error:", error);
    const message = error instanceof Error ? error.message : "Internal filesystem error";
    return NextResponse.json({ error: `Failed to save files: ${message}` }, { status: 500 });
  }
}

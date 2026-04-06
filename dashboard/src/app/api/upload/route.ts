import { NextResponse } from "next/server";
import fs from "fs/promises";
import path from "path";

const UPLOADS_BASE = path.resolve(process.cwd(), "..", "web-uploads");
const SUBMISSIONS_DIR = path.join(UPLOADS_BASE, "submissions");
const TESTERS_DIR = path.join(UPLOADS_BASE, "testers");
const TEMPLATE_DIR = path.join(UPLOADS_BASE, "template");

async function deleteDir(dir: string) {
  try {
    await fs.rm(dir, { recursive: true, force: true });
  } catch {}
}

export async function POST(request: Request) {
  try {
    const formData = await request.formData();
    const submissionFiles = formData.getAll("submissions") as File[];
    const testerFiles = formData.getAll("testers") as File[];
    const templateFiles = formData.getAll("template") as File[];

    if (!submissionFiles.length || !testerFiles.length || !templateFiles.length) {
      return NextResponse.json({ error: "Missing required files." }, { status: 400 });
    }

    // Clean and recreate upload dirs
    await deleteDir(UPLOADS_BASE);
    await fs.mkdir(SUBMISSIONS_DIR, { recursive: true });
    await fs.mkdir(TESTERS_DIR, { recursive: true });
    await fs.mkdir(TEMPLATE_DIR, { recursive: true });

    // 1. Save submission zips
    for (const file of submissionFiles) {
      const dest = path.join(SUBMISSIONS_DIR, file.name.replace(/[^a-zA-Z0-9._-]/g, "_"));
      await fs.writeFile(dest, Buffer.from(await file.arrayBuffer()));
    }

    // 2. Save tester .java files flat into TESTERS_DIR
    //    (browser sends webkitRelativePath as the File name via FormData)
    for (const file of testerFiles) {
      if (!file.name.endsWith(".java")) continue;
      const dest = path.join(TESTERS_DIR, path.basename(file.name));
      await fs.writeFile(dest, Buffer.from(await file.arrayBuffer()));
    }

    // 3. Save template files preserving relative subfolder structure
    //    Relative paths are encoded as the FormData filename (e.g. "Q1/Q1a.java")
    for (const file of templateFiles) {
      const relativePath = file.name.replace(/\\/g, "/");
      const dest = path.join(TEMPLATE_DIR, relativePath);
      await fs.mkdir(path.dirname(dest), { recursive: true });
      await fs.writeFile(dest, Buffer.from(await file.arrayBuffer()));
    }

    return NextResponse.json({
      submissions: submissionFiles.length,
      testers: testerFiles.filter((f) => f.name.endsWith(".java")).length,
      template: templateFiles.length,
    });
  } catch (error: any) {
    console.error("Upload error:", error);
    return NextResponse.json({ error: error.message || "Upload failed." }, { status: 500 });
  }
}

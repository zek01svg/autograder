import { NextResponse } from "next/server";
import { spawn } from "child_process";
import path from "path";

export async function POST() {
  const encoder = new TextEncoder();
  const runBatPath = path.resolve(process.cwd(), "..", "scripts", "run.bat");
  const projectRoot = path.resolve(process.cwd(), "..");

  console.log(`Starting Java Grader from: ${runBatPath}`);

  const stream = new ReadableStream({
    start(controller) {
      const child = spawn("cmd.exe", ["/c", runBatPath, "--submissions", "student-submission"], {
        cwd: projectRoot,
        shell: true,
      });

      child.stdout.on("data", (data) => {
        controller.enqueue(encoder.encode(data.toString()));
      });

      child.stderr.on("data", (data) => {
        // Prefix stderr for visual distinction if needed, or just pipe it
        controller.enqueue(encoder.encode(`\n[ERROR]: ${data.toString()}`));
      });

      child.on("error", (error) => {
        controller.enqueue(encoder.encode(`\n[FAILED TO START]: ${error.message}\n`));
        controller.close();
      });

      child.on("close", (code) => {
        controller.enqueue(encoder.encode(`\n\n[PROCESS EXITED WITH CODE ${code}]\n`));
        controller.close();
      });
    },
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache",
      Connection: "keep-alive",
    },
  });
}

import { spawn } from "child_process";
import path from "path";
import { checkDocker } from "@/lib/docker";

export async function POST() {
  try {
    await checkDocker();
  } catch (error: any) {
    return Response.json({ error: error.message }, { status: 503 });
  }

  const projectRoot = path.resolve(process.cwd(), "..");
  const encoder = new TextEncoder();

  const stream = new ReadableStream({
    start(controller) {
      const child = spawn(
        "java",
        [
          "-cp",
          "out",
          "grader.Main",
          "--submissions",
          "web-uploads/submissions",
          "--testers",
          "web-uploads/testers",
          "--template",
          "web-uploads/template",
          "--workdir",
          "web-work",
          "--output",
          "results/results.csv",
        ],
        { cwd: projectRoot, shell: true },
      );

      child.stdout.on("data", (data) => controller.enqueue(encoder.encode(data.toString())));
      child.stderr.on("data", (data) =>
        controller.enqueue(encoder.encode(`[ERROR]: ${data.toString()}`)),
      );
      child.on("error", (err) => {
        controller.enqueue(encoder.encode(`\n[FAILED TO START]: ${err.message}\n`));
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
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "no-cache",
      Connection: "keep-alive",
    },
  });
}

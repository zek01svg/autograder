import { checkDocker } from "@/lib/docker";

export async function POST() {
  try {
    await checkDocker();
  } catch (error: any) {
    logger.error(`[API/Run] Pre-flight check failed: ${error.message}`);
    return Response.json({ error: error.message }, { status: 503 });
  }

  const encoder = new TextEncoder();
  const isWindows = process.platform === "win32";
  const scriptName = isWindows ? "run.bat" : "run.sh";
  const scriptPath = path.resolve(process.cwd(), "..", "scripts", scriptName);
  const projectRoot = path.resolve(process.cwd(), "..");

  logger.info(`Starting Java Grader from: ${scriptPath} (platform: ${process.platform})`);

  const stream = new ReadableStream({
    start(controller) {
      const child = isWindows
        ? spawn("cmd.exe", ["/c", scriptPath, "--submissions", "student-submission"], {
            cwd: projectRoot,
            shell: true,
          })
        : spawn("sh", [scriptPath, "--submissions", "student-submission"], {
            cwd: projectRoot,
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

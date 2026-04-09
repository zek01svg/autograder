import { generateTestFiles } from "@/lib/ai";

export const maxDuration = 900; // 15 minutes
export const dynamic = "force-dynamic";

export async function POST(request: Request) {
  try {
    const { questionPaper, templateStructure } = await request.json();

    if (!questionPaper || !templateStructure) {
      return Response.json(
        { error: "Question paper and template structure are required." },
        { status: 400 },
      );
    }

    const stream = await generateTestFiles({
      questionPaper,
      templateStructure,
    });

    return new Response(stream, {
      headers: {
        "Content-Type": "text/event-stream",
        "Cache-Control": "no-cache",
        "Connection": "keep-alive",
      },
    });
  } catch (error: any) {
    console.error("AI Generation API Error:", error);

    const status = error.status || 500;
    const retryAfter = error.retryAfterSeconds;

    const headers = new Headers();
    if (retryAfter) {
      headers.set("Retry-After", String(retryAfter));
    }

    return Response.json(
      {
        error: error.message || "Failed to generate test files.",
        retryAfterSeconds: retryAfter,
      },
      { status, headers },
    );
  }
}

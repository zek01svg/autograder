import { NextResponse } from "next/server";
import { generateTestFiles } from "@/lib/ai";

export async function POST(request: Request) {
  try {
    const { questionPaper, templateStructure } = await request.json();

    if (!questionPaper || !templateStructure) {
      return NextResponse.json(
        { error: "Question paper and template structure are required." },
        { status: 400 },
      );
    }

    const apiKey = process.env.GOOGLE_GENERATIVE_AI_API_KEY;
    if (!apiKey || apiKey === "your_api_key_here") {
      return NextResponse.json(
        { error: "Missing GOOGLE_GENERATIVE_AI_API_KEY in server environment." },
        { status: 500 },
      );
    }

    const testFiles = await generateTestFiles({
      questionPaper,
      templateStructure,
      apiKey,
    });

    return NextResponse.json({ files: testFiles });
  } catch (error: any) {
    console.error("AI Generation API Error:", error);

    const status = error.status || 500;
    const retryAfter = error.retryAfterSeconds;

    const headers = new Headers();
    if (retryAfter) {
      headers.set("Retry-After", String(retryAfter));
    }

    return NextResponse.json(
      {
        error: error.message || "Failed to generate test files.",
        retryAfterSeconds: retryAfter,
      },
      { status, headers },
    );
  }
}

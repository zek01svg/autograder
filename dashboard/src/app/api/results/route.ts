import { NextResponse } from "next/server";
import fs from "fs/promises";
import path from "path";

function parseReportHtml(html: string): {
  questionColumns: string[];
  studentMap: Map<
    string,
    { questionScores: Record<string, number>; validationStatus: string; errors: string[] }
  >;
} {
  // Extract question column headers (everything between Validation Status and Total)
  const headerMatch = html.match(/<thead>[\s\S]*?<\/thead>/);
  const questionColumns: string[] = [];
  if (headerMatch) {
    const thMatches = [...headerMatch[0].matchAll(/<th>([^<]+)<\/th>/g)];
    // Skip "Student", "Validation Status", "Total"
    for (const m of thMatches) {
      const col = m[1].trim();
      if (col !== "Student" && col !== "Validation Status" && col !== "Total") {
        questionColumns.push(col);
      }
    }
  }

  // Parse each student row
  const studentMap = new Map<
    string,
    { questionScores: Record<string, number>; validationStatus: string; errors: string[] }
  >();
  const rowRegex = /<tr>([\s\S]*?)<\/tr>/g;
  let rowMatch;
  // skip thead rows by checking if there's a <td>
  while ((rowMatch = rowRegex.exec(html)) !== null) {
    const rowHtml = rowMatch[1];
    if (!rowHtml.includes("<td>") && !rowHtml.includes("<td ")) continue;

    const tdMatches = [...rowHtml.matchAll(/<td[^>]*>([\s\S]*?)<\/td>/g)];
    if (tdMatches.length < 3) continue;

    // First td: student username
    const username = tdMatches[0][1].trim();
    if (!username) continue;

    // Second td: validation status + tooltip
    const statusHtml = tdMatches[1][1];
    const statusMatch = statusHtml.match(/class='status-pill (\w+)'/);
    const validationStatus = statusMatch ? statusMatch[1].toUpperCase() : "UNKNOWN";

    // Extract tooltip bullet points
    const tooltipMatch = statusHtml.match(/<div class='tooltip'>([\s\S]*?)<\/div>/);
    const errors: string[] = [];
    if (tooltipMatch) {
      const bullets = tooltipMatch[1]
        .split("<br>")
        .map((s) => s.replace(/^[•\s]+/, "").trim())
        .filter(Boolean);
      errors.push(...bullets);
    }

    // Remaining tds: per-question scores (skip last which is total)
    const scoreTds = tdMatches.slice(2, tdMatches.length - 1);
    const questionScores: Record<string, number> = {};
    for (let i = 0; i < scoreTds.length && i < questionColumns.length; i++) {
      questionScores[questionColumns[i]] = parseFloat(scoreTds[i][1].trim());
    }

    studentMap.set(username, { questionScores, validationStatus, errors });
  }

  return { questionColumns, studentMap };
}

export async function GET() {
  try {
    // Locate results directory in the project root (one level up from dashboard/)
    const resultsPath = path.join(process.cwd(), "..", "results");
    const csvPath = path.join(resultsPath, "results.csv");
    const htmlPath = path.join(resultsPath, "report.html");

    const [csvContent, htmlContent] = await Promise.all([
      fs.readFile(csvPath, "utf-8").catch(() => null),
      fs.readFile(htmlPath, "utf-8").catch(() => null),
    ]);

    if (!csvContent) {
      return NextResponse.json({ error: "Results not found" }, { status: 404 });
    }

    // Parse HTML for per-question scores, validation status, and errors
    const { questionColumns, studentMap } = htmlContent
      ? parseReportHtml(htmlContent)
      : { questionColumns: [], studentMap: new Map() };

    // Parse the IS442 CSV format
    const lines = csvContent.trim().split("\n");
    const students = lines
      .slice(1)
      .map((line) => {
        const parts = line.split(",");
        if (parts.length < 7) return null;

        const username = parts[1].replace("#", "");
        const htmlData = studentMap.get(username);

        return {
          username,
          score: parseFloat(parts[5]),
          maxScore: parseFloat(parts[6]),
          email: parts[4] === "N/A" ? null : parts[4],
          validationStatus: htmlData?.validationStatus ?? null,
          errors: htmlData?.errors ?? [],
          questionScores: htmlData?.questionScores ?? {},
        };
      })
      .filter(Boolean);

    return NextResponse.json({
      students,
      questionColumns,
      html: htmlContent,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    console.error("Results fetch error:", error);
    return NextResponse.json({ error: "Internal Server Error" }, { status: 500 });
  }
}

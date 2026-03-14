package grader.report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import grader.model.*;
import grader.util.*;

/**
 * Generates grading reports (CSV output).
 * Reads the original IS442-ScoreSheet.csv, fills in computed scores,
 * and writes the results CSV.
 */
public class Reporter {

  private final String[] questionKeys;

  public Reporter(String[] questionKeys) {
    this.questionKeys = questionKeys;
  }

  /**
   * Print a detailed console report of all grade results.
   * Restored for backward compatibility with tests.
   */
  public void printDetailedReport(ArrayList<GradeResult> gradeResults) {
    System.out.println("\n=== Detailed Grading Results ===");
    // Sort by descending total score
    java.util.Collections.sort(gradeResults, (a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));

    for (GradeResult gr : gradeResults) {
      System.out.printf("%-20s: %.1f%n", gr.getStudentId(), gr.getTotalScore());
    }
  }

  public void writeHtmlReport(String path, ArrayList<GradeResult> gradeResults) throws IOException {
    // 1. Calculate Stats
    int count = gradeResults.size();
    double sum = 0;
    double min = count > 0 ? Double.MAX_VALUE : 0;
    double max = 0;

    for (GradeResult gr : gradeResults) {
      double s = gr.getTotalScore();
      sum += s;
      if (s < min)
        min = s;
      if (s > max)
        max = s;
    }
    double avg = count > 0 ? sum / count : 0;

    // 2. Sort Alphabetically
    java.util.Collections.sort(gradeResults, (a, b) -> a.getStudentId().compareToIgnoreCase(b.getStudentId()));

    // 3. Build HTML
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n");
    html.append("<meta charset='UTF-8'>\n<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
    html.append("<title>AutoGrader</title>\n");
    html.append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>\n");

    // Injecting 2026 CSS Styling
    html.append("<style>\n");
    html.append(
        ":root { --primary: oklch(65% 0.2 260); --primary-muted: oklch(65% 0.2 260 / 0.1); --bg: oklch(98% 0.01 260); --card-bg: rgba(255, 255, 255, 0.7); --text: oklch(25% 0.02 260); --text-dim: oklch(50% 0.02 260); --border: oklch(90% 0.01 260); --success: oklch(65% 0.15 150); --danger: oklch(60% 0.18 25); --radius: 1.25rem; --glass-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.07); }\n");
    html.append(
        "[data-theme='dark'] { --bg: oklch(12% 0.02 260); --card-bg: rgba(30, 41, 59, 0.5); --text: oklch(95% 0.01 260); --text-dim: oklch(75% 0.01 260); --border: oklch(25% 0.02 260); --primary: oklch(75% 0.15 260); }\n");
    html.append(
        "body { font-family: 'Inter Variable', system-ui, sans-serif; background: var(--bg); background-image: radial-gradient(at 0% 0%, var(--primary-muted) 0px, transparent 50%), radial-gradient(at 100% 100%, var(--primary-muted) 0px, transparent 50%); color: var(--text); margin: 0; padding: clamp(1rem, 5vw, 4rem); min-height: 100vh; transition: all 0.4s ease; }\n");
    html.append(".container { max-width: 1100px; margin: 0 auto; }\n");
    html.append(
        "header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 3rem; }\n");
    html.append(
        "h1 { font-size: clamp(1.5rem, 3vw, 2.5rem); font-weight: 850; letter-spacing: -0.04em; margin: 0; background: linear-gradient(to right, var(--primary), var(--text)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }\n");
    html.append(
        ".stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 1.25rem; margin-bottom: 2.5rem; }\n");
    html.append(
        ".stat-card { background: var(--card-bg); backdrop-filter: blur(12px); padding: 1.5rem; border-radius: var(--radius); border: 1px solid var(--border); box-shadow: var(--glass-shadow); }\n");
    html.append(
        ".stat-label { font-size: 0.7rem; font-weight: 700; color: var(--text-dim); text-transform: uppercase; }\n");
    html.append(".stat-value { font-size: 2rem; font-weight: 800; margin-top: 0.25rem; }\n");
    html.append(
        ".chart-container { background: var(--card-bg); backdrop-filter: blur(12px); padding: 1.5rem; border-radius: var(--radius); border: 1px solid var(--border); box-shadow: var(--glass-shadow); margin-bottom: 2.5rem; height: 300px; }\n");
    html.append(
        ".table-container { background: var(--card-bg); backdrop-filter: blur(12px); border-radius: var(--radius); border: 1px solid var(--border); box-shadow: var(--glass-shadow); overflow: visible; }\n");
    html.append("table { width: 100%; border-collapse: collapse; }\n");
    html.append(
        "th { text-align: left; padding: 1.25rem; font-size: 0.8rem; color: var(--text-dim); border-bottom: 1px solid var(--border); text-transform: uppercase; letter-spacing: 0.05em; }\n");
    html.append(
        "td { padding: 1.25rem; border-bottom: 1px solid var(--border); font-size: 0.9rem; position: relative; }\n");
    html.append(
        ".status-pill { display: inline-flex; align-items: center; padding: 0.4rem 0.8rem; border-radius: 2rem; font-size: 0.75rem; font-weight: 700; cursor: help; }\n");
    html.append(".status-pill.pass { background: oklch(from var(--success) l c h / 0.15); color: var(--success); }\n");
    html.append(
        ".status-pill.fail { background: oklch(from var(--danger) l c h / 0.15); color: var(--danger); text-decoration: underline dotted; }\n");
    html.append(
        ".tooltip { visibility: hidden; position: absolute; bottom: 125%; left: 50%; transform: translateX(-50%); background: var(--text); color: var(--bg); padding: 0.75rem 1rem; border-radius: 0.75rem; width: 240px; font-size: 0.75rem; font-weight: 400; line-height: 1.4; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.2); z-index: 100; opacity: 0; transition: opacity 0.3s; pointer-events: none; }\n");
    html.append(
        ".tooltip::after { content: ''; position: absolute; top: 100%; left: 50%; margin-left: -5px; border-width: 5px; border-style: solid; border-color: var(--text) transparent transparent transparent; }\n");
    html.append(".status-pill.fail:hover + .tooltip { visibility: visible; opacity: 1; }\n");
    html.append(".score { font-weight: 800; color: var(--primary); }\n");
    html.append(
        ".theme-toggle { background: var(--card-bg); border: 1px solid var(--border); padding: 0.6rem 1.2rem; border-radius: 0.75rem; cursor: pointer; color: var(--text); font-weight: 600; }\n");
    html.append("</style>\n</head>\n<body>\n");

    html.append("<div class='container'>\n<header>\n<h1>AutoGrader</h1>\n");
    html.append("<button class='theme-toggle' onclick='toggleTheme()'>🌓 Theme</button>\n</header>\n");

    // JS for Toggle
    html.append("<script>\n");
    html.append("function toggleTheme() {\n");
    html.append("  const html = document.documentElement;\n");
    html.append("  const current = html.getAttribute('data-theme');\n");
    html.append("  const next = current === 'dark' ? 'light' : 'dark';\n");
    html.append("  html.setAttribute('data-theme', next);\n");
    html.append("  localStorage.setItem('theme', next);\n");
    html.append("  if (window.myChart) window.myChart.update();\n");
    html.append("}\n");
    html.append("if (localStorage.getItem('theme') === 'dark') {\n");
    html.append("  document.documentElement.setAttribute('data-theme', 'dark');\n");
    html.append("}\n");
    html.append("</script>\n");

    // Stats Grid
    html.append("<div class='stats-grid'>\n");
    html.append("<div class='stat-card'><div class='stat-label'>Submissions</div><div class='stat-value'>")
        .append(count).append("</div></div>\n");
    html.append("<div class='stat-card'><div class='stat-label'>Average</div><div class='stat-value'>")
        .append(String.format("%.1f", avg)).append("</div></div>\n");
    html.append("<div class='stat-card'><div class='stat-label'>Min</div><div class='stat-value'>")
        .append(String.format("%.1f", min)).append("</div></div>\n");
    html.append("<div class='stat-card'><div class='stat-label'>Max</div><div class='stat-value'>")
        .append(String.format("%.1f", max)).append("</div></div>\n");
    html.append("</div>\n");
    html.append("<div class='chart-container'>\n<canvas id='bellCurveChart'></canvas>\n</div>\n");

    // Table Header (Added 'Status' Column)
    html.append(
        "<div class='table-container'>\n<table>\n<thead>\n<tr>\n<th>Student</th>\n<th>Validation Status</th>\n");
    for (String q : questionKeys) { // Assuming questionKeys is available in your class scope
      html.append("<th>").append(q).append("</th>\n");
    }
    html.append("<th>Total</th>\n</tr>\n</thead>\n<tbody>\n");

    // Table Body & Dynamic Status Logic
    StringBuilder scoresJs = new StringBuilder("const scores = [");
    boolean firstScore = true;
    for (GradeResult gr : gradeResults) {
      if (!firstScore)
        scoresJs.append(",");
      scoresJs.append(gr.getTotalScore());
      firstScore = false;

      html.append("<tr>\n");
      html.append("<td>").append(gr.getStudentId()).append("</td>\n");

      // Dynamic Status Column
      html.append("<td>\n");
      java.util.List<String> anomalies = gr.getAnomalies();
      if (anomalies == null || anomalies.isEmpty()) {
        html.append("<span class='status-pill pass'>PASS</span>\n");
      } else {
        html.append("<span class='status-pill fail'>FAIL</span>\n");
        html.append("<div class='tooltip'>\n");
        for (String a : anomalies) {
          // Formatting anomalies separated by <br> for readability in tooltip
          html.append("• ").append(a.replace("\"", "&quot;")).append("<br>\n");
        }
        html.append("</div>\n");
      }
      html.append("</td>\n");

      for (String q : questionKeys) {
        html.append("<td>").append(gr.getQuestionScore(q)).append("</td>\n");
      }
      html.append("<td class='score'>").append(gr.getTotalScore()).append("</td>\n");
      html.append("</tr>\n");
    }
    scoresJs.append("];\n");

    html.append("</tbody>\n</table>\n</div>\n");

    // Dynamic Chart Logic
    html.append("<script>\n");
    html.append(scoresJs.toString());
    html.append("if (scores.length > 0) {\n");
    html.append("  const mean = scores.reduce((a, b) => a + b) / scores.length;\n");
    html.append(
        "  const stdDev = Math.sqrt(scores.map(x => Math.pow(x - mean, 2)).reduce((a, b) => a + b) / scores.length) || 1;\n");
    html.append(
        "  function normalPDF(x, mean, stdDev) { return (1 / (stdDev * Math.sqrt(2 * Math.PI))) * Math.exp(-0.5 * Math.pow((x - mean) / stdDev, 2)); }\n");
    html.append("  const chartData = [];\n");
    html.append("  const sortedScores = [...scores].sort((a,b) => a-b);\n");
    html.append("  const minScore = Math.min(0, sortedScores[0] - 5);\n");
    html.append("  const maxScore = sortedScores[sortedScores.length - 1] + 5;\n");
    html.append(
        "  for (let i = minScore; i <= maxScore; i += 0.5) { chartData.push({ x: i, y: normalPDF(i, mean, stdDev) }); }\n");
    html.append("  const ctx = document.getElementById('bellCurveChart').getContext('2d');\n");
    html.append("  window.myChart = new Chart(ctx, {\n");
    html.append(
        "    type: 'scatter', data: { datasets: [{ label: 'Distribution', data: chartData, showLine: true, borderColor: '#4f46e5', backgroundColor: 'rgba(79, 70, 229, 0.1)', fill: true, pointRadius: 0, borderWidth: 3, tension: 0.4 }] },\n");
    html.append(
        "    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: { enabled: false } }, scales: { y: { display: false }, x: { grid: { display: false }, ticks: { color: '#888' }, title: { display: true, text: 'Score', color: '#888' } } } }\n");
    html.append("  });\n}\n");
    html.append("</script>\n");

    html.append("</div>\n</body>\n</html>");

    // Write to file
    try (java.io.FileWriter fw = new java.io.FileWriter(path)) {
      fw.write(html.toString());
    }
  }

  public void writeCsv(String scoreSheetPath, String outputPath,
      ArrayList<GradeResult> gradeResults) throws IOException {

    ArrayList<CsvUtil.ScoreRow> rows;
    File template = new File(scoreSheetPath);
    if (template.exists()) {
      rows = CsvUtil.readScoreSheet(scoreSheetPath);
    } else {
      // Template missing: generate basic rows from results
      rows = new ArrayList<>();
      for (GradeResult gr : gradeResults) {
        rows.add(new CsvUtil.ScoreRow(
            "N/A", "#" + gr.getStudentId(), "_", "_", "N/A", "", "20", "#"));
      }
    }

    // Match grade results to score rows by username
    for (CsvUtil.ScoreRow row : rows) {
      String username = row.cleanUsername();
      GradeResult match = null;

      for (GradeResult gr : gradeResults) {
        if (gr.getStudentId().equals(username)) {
          match = gr;
          break;
        }
      }

      if (match != null) {
        double total = match.getTotalScore();
        if (total == Math.floor(total) && !Double.isInfinite(total)) {
          row.gradeNumerator = String.valueOf((int) total);
        } else {
          row.gradeNumerator = String.valueOf(total);
        }
      }
    }

    CsvUtil.writeScoreSheet(outputPath, rows);
  }
}

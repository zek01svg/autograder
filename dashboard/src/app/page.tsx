"use client";

import { useState, useRef, useEffect } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTheme } from "next-themes";
import { UploadZone } from "@/components/UploadZone";
import { TestReviewer } from "@/components/TestReviewer";
import { Button } from "@/components/ui/button";
import {
  Play,
  CheckCircle2,
  Loader2,
  Terminal,
  BarChart3,
  TrendingUp,
  Cpu,
  Sun,
  Moon,
} from "lucide-react";
import { ScoreDistribution } from "@/components/ScoreDistribution";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { TestFile } from "@/lib/schema";
import { splitQuestionPaper, getRelevantFiles } from "@/lib/utils";

type Step = "upload" | "review" | "run";

export default function DashboardPage() {
  const [currentStep, setCurrentStep] = useState<Step>("upload");
  const [generatedFiles, setGeneratedFiles] = useState<TestFile[]>([]);
  const [executionOutput, setExecutionOutput] = useState<string>("");
  const [progress, setProgress] = useState<{ current: number; total: number } | null>(null);
  const [results, setResults] = useState<{
    students: any[];
    questionColumns: string[];
    html: string;
  } | null>(null);
  const [uploadMode, setUploadMode] = useState<"ai" | "direct">("ai");
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  const terminalEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (terminalEndRef.current) {
      terminalEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [executionOutput]);

  // 1. Generate Mutation
  const generateMutation = useMutation({
    mutationFn: async (data: { questionPaper: string; templateStructure: string }) => {
      const sections = splitQuestionPaper(data.questionPaper);
      const allFiles = data.templateStructure.split("\n").filter(Boolean);
      const allFilesMap = new Map<string, TestFile>();

      for (let i = 0; i < sections.length; i++) {
        const sectionText = sections[i];
        setProgress({ current: i + 1, total: sections.length });

        const prunedContext = getRelevantFiles(sectionText, allFiles);

        const res = await fetch("/api/generate", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            questionPaper: sectionText,
            templateStructure: prunedContext || data.templateStructure,
          }),
        });

        if (!res.ok) {
          const err = await res.json();
          throw new Error(err.error || `Failed at section ${i + 1}`);
        }

        const sectionData = await res.json();
        if (sectionData.files) {
          sectionData.files.forEach((file: TestFile) => {
            allFilesMap.set(file.filename, file);
          });
        }

        if (i < sections.length - 1) {
          await new Promise((r) => setTimeout(r, 2000));
        }
      }

      return { files: Array.from(allFilesMap.values()) };
    },
    onSuccess: (data) => {
      setGeneratedFiles(data.files);
      setCurrentStep("review");
      setProgress(null);
    },
    onError: () => {
      setProgress(null);
    },
  });

  // 2. Direct Upload Mutation (submissions zips + tester .java files + template folder files)
  const directUploadMutation = useMutation({
    mutationFn: async (data: {
      submissionFiles: File[];
      testerFiles: File[];
      templateFiles: File[];
    }) => {
      const form = new FormData();
      data.submissionFiles.forEach((f) => form.append("submissions", f, f.name));
      // Send tester files by basename only
      data.testerFiles.forEach((f) => form.append("testers", f, f.name.split("/").pop() ?? f.name));
      // Send template files with their relative path as the filename so the server can reconstruct the tree
      data.templateFiles.forEach((f) => {
        const rel = (f as any).webkitRelativePath || f.name;
        // Strip the top-level folder name (e.g. "RenameToYourUsername/Q1/Q1a.java" → "Q1/Q1a.java")
        const stripped = rel.split("/").slice(1).join("/") || f.name;
        form.append("template", f, stripped);
      });
      const res = await fetch("/api/upload", { method: "POST", body: form });
      if (!res.ok) throw new Error((await res.json()).error || "Upload failed");
      return res.json();
    },
    onSuccess: () => {
      setCurrentStep("run");
    },
  });

  // 3. Save Mutation
  const saveMutation = useMutation({
    mutationFn: async (files: TestFile[]) => {
      const res = await fetch("/api/save", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ files }),
      });
      if (!res.ok) throw new Error("Failed to save approved tests");
      return res.json();
    },
    onSuccess: () => {
      setCurrentStep("run");
    },
  });

  // 4. Run Mutation (uses /api/grade for direct uploads, /api/run for AI/manual)
  const runMutation = useMutation({
    mutationFn: async () => {
      const endpoint = uploadMode === "direct" ? "/api/grade" : "/api/run";
      const res = await fetch(endpoint, { method: "POST" });
      if (!res.ok) throw new Error("Pipeline run failed");

      const reader = res.body?.getReader();
      if (!reader) throw new Error("No response body");

      setExecutionOutput("");
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = new TextDecoder().decode(value);
        setExecutionOutput((prev) => prev + chunk);
      }

      const resultsRes = await fetch("/api/results");
      if (resultsRes.ok) {
        const data = await resultsRes.json();
        setResults(data);
      }
    },
  });

  return (
    <div className="min-h-screen bg-background text-foreground font-sans relative overflow-x-hidden">
      {/* Decorative Background Elements */}
      <div className="absolute top-0 left-0 w-full h-[500px] bg-linear-to-b from-indigo-500/5 to-transparent pointer-events-none" />
      <div className="absolute top-[20%] -right-[10%] w-[40%] h-[40%] bg-indigo-600/5 blur-[120px] rounded-none pointer-events-none" />

      <header className="flex border-b border-border bg-background/50 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto w-full px-8 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-indigo-600 flex items-center justify-center rounded-none shadow-lg shadow-indigo-500/20">
              <Cpu className="text-primary-foreground w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-black text-foreground tracking-widest font-heading uppercase">
                Autograder
              </h1>
              <div className="flex gap-4 mt-1">
                {(uploadMode === "direct" ? ["upload", "run"] : ["upload", "review", "run"]).map(
                  (step, i) => (
                    <div
                      key={step}
                      className={`text-[9px] font-black uppercase tracking-widest flex items-center gap-1.5 transition-colors ${currentStep === step ? "text-indigo-600 dark:text-indigo-400" : "text-muted-foreground"}`}
                    >
                      <span
                        className={`w-3.5 h-3.5 flex items-center justify-center border ${currentStep === step ? "border-indigo-600 dark:border-indigo-400 bg-indigo-600/10 dark:bg-indigo-400/10" : "border-border"} rounded-none`}
                      >
                        {i + 1}
                      </span>
                      {step}
                    </div>
                  ),
                )}
              </div>
            </div>
          </div>

          {/* Theme Toggle */}
          <Button
            variant="ghost"
            size="icon"
            className="rounded-none border border-border bg-card hover:bg-accent hover:text-accent-foreground"
            onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          >
            {mounted &&
              (theme === "dark" ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />)}
          </Button>
        </div>
      </header>

      <div className="max-w-6xl mx-auto space-y-16 p-6 md:p-12">
        {/* Global Pending State Overlay */}
        {generateMutation.isPending && (
          <div className="fixed inset-0 bg-background/90 backdrop-blur-sm z-50 flex flex-col items-center justify-center gap-8">
            <Loader2 className="w-16 h-16 text-indigo-500 animate-spin" />
            <div className="text-center space-y-3">
              <h3 className="text-2xl font-black text-foreground font-heading tracking-widest uppercase">
                {progress
                  ? `Synthesizing ${progress.current}/${progress.total}`
                  : "Initializing AI Engine"}
              </h3>
              {progress && (
                <div className="w-64 h-1 bg-muted mt-6 mx-auto">
                  <div
                    className="h-full bg-indigo-500 transition-all duration-700"
                    style={{ width: `${(progress.current / progress.total) * 100}%` }}
                  />
                </div>
              )}
            </div>
          </div>
        )}

        <main className="min-h-[40vh]">
          {/* 1. Upload Step */}
          {currentStep === "upload" && !generateMutation.isPending && (
            <div className="space-y-12">
              <div className="mb-12">
                <h2 className="text-3xl font-black text-foreground tracking-tight font-heading uppercase">
                  Assignment
                </h2>
                <p className="text-muted-foreground mt-2 font-medium">
                  Upload assignment to generate tests.
                </p>
              </div>
              <UploadZone
                onFilesSelected={(data) => {
                  if (data.mode === "generate" && data.questionPaper) {
                    setUploadMode("ai");
                    generateMutation.mutate({
                      questionPaper: data.questionPaper,
                      templateStructure: data.templateStructure!,
                    });
                  } else if (
                    data.mode === "direct" &&
                    data.submissionFiles &&
                    data.testerFiles &&
                    data.templateFiles
                  ) {
                    setUploadMode("direct");
                    directUploadMutation.mutate({
                      submissionFiles: data.submissionFiles,
                      testerFiles: data.testerFiles,
                      templateFiles: data.templateFiles,
                    });
                  }
                }}
              />

              {(generateMutation.isError || directUploadMutation.isError) && (
                <Card className="bg-red-500/10 border-red-500/20 border-2 max-w-2xl rounded-none">
                  <CardHeader>
                    <CardTitle className="text-red-400 font-black uppercase tracking-widest text-xs">
                      Error
                    </CardTitle>
                    <CardDescription className="text-red-300/70 font-mono text-xs uppercase">
                      {((generateMutation.error || directUploadMutation.error) as Error)?.message}
                    </CardDescription>
                  </CardHeader>
                </Card>
              )}
            </div>
          )}

          {/* 2. Review Step */}
          {currentStep === "review" && (
            <div className="space-y-8">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
                <div>
                  <h2 className="text-3xl font-black text-foreground tracking-tight font-heading uppercase">
                    Review
                  </h2>
                  <p className="text-muted-foreground font-medium">
                    Verify generated JUnit assertions.
                  </p>
                </div>
                {saveMutation.isPending && (
                  <div className="flex items-center gap-3 text-indigo-600 dark:text-indigo-400 font-bold animate-pulse text-xs uppercase tracking-widest">
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Persisting codebase...
                  </div>
                )}
              </div>
              <div className="flex flex-col gap-10">
                <TestReviewer
                  files={generatedFiles}
                  onSave={() => saveMutation.mutate(generatedFiles)}
                  isLoading={saveMutation.isPending}
                />
              </div>
            </div>
          )}

          {/* 3. Run Step */}
          {currentStep === "run" && (
            <div className="space-y-16">
              {!results ? (
                <div className="max-w-3xl mx-auto w-full flex flex-col items-center gap-12 py-12">
                  <div className="text-center">
                    <div className="w-20 h-20 bg-indigo-600/10 border-2 border-indigo-500 flex items-center justify-center rounded-none mb-6 mx-auto group">
                      <CheckCircle2 className="w-10 h-10 text-indigo-600 dark:text-indigo-400 group-hover:scale-110 transition-transform" />
                    </div>
                    <h2 className="text-4xl font-black text-foreground font-heading uppercase tracking-widest">
                      Execute Pipeline
                    </h2>
                    <p className="text-muted-foreground mt-3 font-mono text-xs uppercase tracking-[0.2em]">
                      Tests committed. Standing by for execution.
                    </p>
                  </div>

                  <div className="w-full relative group">
                    <div className="absolute inset-0 bg-indigo-500/5 opacity-0 group-hover:opacity-100 transition-opacity" />
                    <Button
                      variant="outline"
                      className="w-full h-24 border-2 border-border bg-card hover:bg-accent hover:border-indigo-500 text-foreground font-black uppercase tracking-[0.3em] transition-all rounded-none relative z-10"
                      onClick={() => runMutation.mutate()}
                      disabled={runMutation.isPending}
                    >
                      {runMutation.isPending ? (
                        <span className="flex items-center gap-4 text-xs">
                          <Loader2 className="w-6 h-6 animate-spin" />
                          RUNNING PIPELINE...
                        </span>
                      ) : (
                        <span className="flex items-center gap-4 text-xs">
                          <Play className="w-6 h-6 text-indigo-600 dark:text-indigo-400" />
                          START EXECUTION
                        </span>
                      )}
                    </Button>
                  </div>
                </div>
              ) : null}

              {/* Console Output */}
              {(runMutation.isPending || (executionOutput && !results)) && (
                <Card className="bg-card border-2 border-border rounded-none overflow-hidden shadow-2xl">
                  <div className="bg-muted px-6 py-4 border-b border-border flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <Terminal className="w-4 h-4 text-muted-foreground" />
                      <span className="text-[10px] font-black text-muted-foreground uppercase tracking-widest">
                        System Pipeline Log
                      </span>
                    </div>
                    <div className="flex gap-1.5 font-bold">
                      <div className="w-2.5 h-2.5 bg-muted-foreground/20 border border-border rounded-none" />
                      <div className="w-2.5 h-2.5 bg-muted-foreground/20 border border-border rounded-none" />
                      <div className="w-2.5 h-2.5 bg-muted-foreground/20 border border-border rounded-none" />
                    </div>
                  </div>
                  <CardContent className="p-0">
                    <div className="bg-black p-8 h-[500px] overflow-y-auto font-mono text-xs leading-relaxed text-zinc-400 scrollbar-thin scrollbar-thumb-zinc-800">
                      {executionOutput.split("\n").map((line, i) => (
                        <div key={i} className="mb-1 flex gap-4 group">
                          <span className="text-zinc-700 select-none w-8 text-right font-black uppercase text-[9px] mt-0.5">
                            {i + 1}
                          </span>
                          <span className="flex-1 break-all group-hover:text-zinc-200 transition-colors">
                            {line}
                          </span>
                        </div>
                      ))}
                      <div ref={terminalEndRef} />
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Reporting Dashboard */}
              {results && (
                <div className="space-y-16">
                  <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 pb-8 border-b border-border">
                    <div className="space-y-2">
                      <h3 className="text-4xl font-black text-foreground tracking-tighter flex items-center gap-4 font-heading uppercase">
                        <BarChart3 className="w-10 h-10 text-indigo-500" />
                        Results
                      </h3>
                    </div>
                    <Button
                      variant="secondary"
                      className="h-12 px-8 font-black bg-secondary text-secondary-foreground hover:bg-secondary/80 border-none rounded-none"
                      onClick={() => {
                        const cols = results.questionColumns;
                        const header = ["Username", "Validation", "Errors", ...cols, "Total"].join(
                          ",",
                        );
                        const rows = results.students.map((s) =>
                          [
                            s.username,
                            s.validationStatus ?? "",
                            `"${(s.errors as string[]).join("; ").replace(/"/g, '""')}"`,
                            ...cols.map((c: string) => s.questionScores[c] ?? ""),
                            s.score,
                          ].join(","),
                        );
                        const csv = [header, ...rows].join("\n");
                        const a = document.createElement("a");
                        a.href = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
                        a.download = "results.csv";
                        a.click();
                      }}
                    >
                      DOWNLOAD RAW DATA (.CSV)
                    </Button>
                  </div>

                  <ScoreDistribution students={results.students} />

                  <div className="space-y-6">
                    <h4 className="text-2xl font-black text-foreground flex items-center gap-4 px-2 tracking-tight font-heading uppercase">
                      <TrendingUp className="w-7 h-7 text-green-500" />
                      Individual Records
                    </h4>
                    <Card className="bg-card border-border overflow-hidden shadow-2xl rounded-none border-2">
                      <div className="overflow-x-auto font-sans">
                        <table className="w-full text-left border-collapse">
                          <thead>
                            <tr className="bg-muted/50 border-b border-border">
                              <th className="px-6 py-4 text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground w-48">
                                Student
                              </th>
                              <th className="px-6 py-4 text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground">
                                Validation
                              </th>
                              {results.questionColumns.map((col) => (
                                <th
                                  key={col}
                                  className="px-6 py-4 text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground text-right"
                                >
                                  {col}
                                </th>
                              ))}
                              <th className="px-6 py-4 text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground text-right w-28">
                                Total
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            {results.students.map((s, i) => (
                              <tr
                                key={i}
                                className="border-b border-border/30 hover:bg-muted/50 transition-all duration-300"
                              >
                                <td className="px-6 py-4 font-mono text-foreground font-black text-sm">
                                  {s.username}
                                </td>
                                <td className="px-6 py-4">
                                  {s.validationStatus === "PASS" ? (
                                    <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-green-500/15 text-green-600 dark:text-green-400">
                                      PASS
                                    </span>
                                  ) : s.validationStatus === "FAIL" ? (
                                    <div className="relative group inline-block">
                                      <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-red-500/15 text-red-500 dark:text-red-400 underline decoration-dotted cursor-help">
                                        FAIL
                                      </span>
                                      {s.errors.length > 0 && (
                                        <div className="absolute left-0 bottom-full mb-2 hidden group-hover:block z-50 w-64 bg-popover text-popover-foreground text-xs rounded-lg shadow-xl p-3 border border-border leading-relaxed">
                                          {s.errors.map((e: string, j: number) => (
                                            <div key={j}>• {e}</div>
                                          ))}
                                        </div>
                                      )}
                                    </div>
                                  ) : (
                                    <span className="text-muted-foreground text-xs">—</span>
                                  )}
                                </td>
                                {results.questionColumns.map((col) => (
                                  <td
                                    key={col}
                                    className="px-6 py-4 text-right text-sm text-foreground"
                                  >
                                    {s.questionScores[col] !== undefined
                                      ? s.questionScores[col].toFixed(1)
                                      : "—"}
                                  </td>
                                ))}
                                <td className="px-6 py-4 text-right font-black text-indigo-600 dark:text-indigo-400 text-sm">
                                  {s.score.toFixed(1)}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </Card>
                  </div>
                </div>
              )}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

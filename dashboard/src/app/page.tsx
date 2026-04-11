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
  Download,
} from "lucide-react";
import { ScoreDistribution } from "@/components/ScoreDistribution";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { TestFile } from "@/lib/schema";
import JSZip from "jszip";
import { toast } from "sonner";

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
  const [preloadedTesters, setPreloadedTesters] = useState<File[]>([]);
  const [preloadedTemplates, setPreloadedTemplates] = useState<File[]>([]);
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);
  const [thinkingText, setThinkingText] = useState<string>("");
  const [generationStatus, setGenerationStatus] = useState<string>("Initializing AI Engine");
  const [filesDetected, setFilesDetected] = useState<number>(0);
  const [generationStartTime, setGenerationStartTime] = useState<number | null>(null);
  const [elapsedTime, setElapsedTime] = useState<number>(0);

  const terminalEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (terminalEndRef.current) {
      terminalEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [executionOutput]);

  // Elapsed time ticker
  useEffect(() => {
    if (!generationStartTime) return;
    const interval = setInterval(() => {
      setElapsedTime(Math.floor((Date.now() - generationStartTime) / 1000));
    }, 1000);
    return () => clearInterval(interval);
  }, [generationStartTime]);

  // 1. Generate Mutation
  const generateMutation = useMutation({
    mutationFn: async (data: { questionPaper: string; templateStructure: string }) => {
      setThinkingText("Initializing local AI...");
      setGenerationStatus("Connecting to Ollama");
      setFilesDetected(0);
      setGenerationStartTime(Date.now());
      setElapsedTime(0);
      
      const response = await fetch("/api/generate", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          questionPaper: data.questionPaper,
          templateStructure: data.templateStructure,
        }),
      });

      if (!response.ok) {
        throw new Error(`Failed to generate: ${response.statusText}`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error("Response body is not readable");
      }

      const decoder = new TextDecoder();
      let buffer = "";
      let fullContent = "";
      let currentThinking = "";
      let finalFiles: TestFile[] = [];
      let fixedFiles: TestFile[] | null = null;

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split("\n");
          buffer = lines.pop() || "";

          for (const line of lines) {
            const trimmed = line.trim();
            if (!trimmed || !trimmed.startsWith("data: ")) continue;
            
            const dataStr = trimmed.slice(6);
            if (dataStr === "[DONE]") continue;

            try {
              const event = JSON.parse(dataStr);
              if (event.type === "content") {
                fullContent += event.delta;
                
                // Try to extract "thinking" for immediate feedback
                const thinkingMatch = fullContent.match(/"thinking":\s*"((?:[^"\\]|\\.)*)"/);
                if (thinkingMatch && thinkingMatch[1]) {
                  currentThinking = thinkingMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"');
                  setThinkingText(currentThinking);
                  setGenerationStatus("Analysing requirements");
                }

                // Track file generation progress by counting completed "filename" entries
                const fileMatches = fullContent.match(/"filename"\s*:\s*"[^"]+"/g);
                const detectedCount = fileMatches ? fileMatches.length : 0;
                if (detectedCount > filesDetected) {
                  setFilesDetected(detectedCount);
                  const lastFile = fileMatches![fileMatches!.length - 1].match(/"([^"]+)"\s*$/)?.[1] || "";
                  setGenerationStatus(`Generating ${lastFile}`);
                }

                // Detect when code blocks are being written
                if (fullContent.includes('"code"') && detectedCount > 0) {
                  const codeMatches = fullContent.match(/"code"\s*:\s*"/g);
                  if (codeMatches && codeMatches.length > (fileMatches?.length || 0) - 1) {
                    setGenerationStatus(`Writing test cases (${detectedCount} of ~5 files)`);
                  }
                }
              } else if (event.type === "fixed_files" && event.files) {
                // Post-processed files from the server with flat filenames and expanded code
                fixedFiles = event.files;
              }
            } catch (e) {
              console.warn("Failed to parse stream chunk:", dataStr);
            }
          }
        }

        // Final parse of the complete JSON object
        if (fullContent) {
          console.log("[AI Generate] Raw content length:", fullContent.length);
          try {
            const finalParsed = JSON.parse(fullContent);
            finalFiles = finalParsed.files || [];
            currentThinking = finalParsed.thinking || currentThinking;
          } catch (e) {
            console.error("Failed to parse final JSON content:", e);
            throw new Error("AI returned invalid JSON formatting. Please try again.");
          }
        }
      } finally {
        reader.releaseLock();
      }

      // Prefer the server-side post-processed files (flat filenames, expanded code)
      const resultFiles = (fixedFiles || finalFiles).map((f: any) => {
        // Ensure flat filename
        const basename = (f.filename || "").split("/").pop() || f.filename;
        // Derive questionRef from filename if not present (e.g., Q1aTester.java -> Q1a)
        const questionRef = f.questionRef || basename.replace("Tester.java", "");
        return { ...f, filename: basename, questionRef, explanation: f.explanation || "" };
      });

      if (resultFiles.length === 0) {
        throw new Error("AI failed to generate any valid test files.");
      }

      return { files: resultFiles as TestFile[], thinking: currentThinking };
    },
    onSuccess: (data) => {
      setGeneratedFiles(data.files);
      setCurrentStep("review");
      setThinkingText("");
      setProgress(null);
      setGenerationStartTime(null);
      setGenerationStatus("");
      toast.success("AI test generation completed successfully.");
    },
    onError: (error: any) => {
      setThinkingText("");
      setProgress(null);
      setGenerationStartTime(null);
      setGenerationStatus("");
      console.error("Generation failed:", error);
      toast.error(error.message || "AI generation failed.");
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
      toast.success("Files uploaded successfully.");
    },
    onError: (error: any) => {
      toast.error(error.message || "Upload failed.");
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
      // Convert generated test files to File objects for the pipeline
      // Use flat basenames so the upload route places them correctly in web-uploads/testers/
      const testerFileObjects = generatedFiles.map(
        (f) => {
          const basename = f.filename.split("/").pop() || f.filename;
          return new File([f.code], basename, { type: "text/x-java" });
        }
      );
      setPreloadedTesters(testerFileObjects);
      setUploadMode("direct");
      setCurrentStep("upload"); // Transition back to Upload to get submissions
      toast.success("Tests saved. Please upload student submissions.");
    },
    onError: (error: any) => {
      toast.error(error.message || "Failed to save codebase.");
    },
  });

  // 4. Run Mutation (uses /api/grade for direct uploads, /api/run for AI/manual)
  const runMutation = useMutation({
    mutationFn: async () => {
      const endpoint = uploadMode === "direct" ? "/api/grade" : "/api/run";
      const res = await fetch(endpoint, { method: "POST" });
      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.error || "Pipeline run failed. Please try again.");
      }

      const reader = res.body?.getReader();
      if (!reader) throw new Error("No response body");

      setExecutionOutput("");
      let fullOutput = "";
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = new TextDecoder().decode(value);
        fullOutput += chunk;
        setExecutionOutput((prev) => prev + chunk);
      }

      // Check the entire output for the exit code with a more flexible regex
      const exitCodeMatch = fullOutput.match(/\[PROCESS EXITED WITH CODE\s*(\d+)\]/i);
      const exitCode = exitCodeMatch ? parseInt(exitCodeMatch[1], 10) : 0;

      // Also check specifically for known fatal strings if the exit code is missing or zero
      const isDockerError = fullOutput.includes("Docker engine is not running") || fullOutput.includes("Cannot connect to the Docker daemon");

      console.log(`[Pipeline] Finished with code: ${exitCode}, dockerError: ${isDockerError}`);

      if (exitCode !== 0 || isDockerError) {
        setResults(null);
        let errorMsg = `Pipeline execution failed (Exit Code ${exitCode}). Check terminal for details.`;
        
        if (isDockerError) {
          errorMsg = "Docker engine is offline. Please start Docker Desktop and try again.";
        } else if (fullOutput.includes("Compilation failed")) {
          errorMsg = "Java compilation failed. Check the submission files for syntax errors.";
        }
        
        throw new Error(errorMsg);
      }

      const resultsRes = await fetch("/api/results");
      if (resultsRes.ok) {
        const data = await resultsRes.json();
        setResults(data);
        toast.success("Pipeline executed and results captured.");
      }
    },
    onError: (error: any) => {
      toast.error(error.message || "Pipeline execution failure.");
    },
  });

  return (
    <div className="min-h-screen bg-background text-foreground font-sans relative overflow-x-hidden">
      {/* Decorative Background Elements */}
      <div className="absolute top-0 left-0 w-full h-[500px] bg-linear-to-b from-indigo-500/5 to-transparent pointer-events-none" />
      <div className="absolute top-[20%] -right-[10%] w-[40%] h-[40%] bg-indigo-600/5 blur-[120px] rounded-none pointer-events-none" />

      <header className="flex border-b border-border bg-background/50 backdrop-blur-md sticky top-0 z-[60]">
        <div className="max-w-7xl mx-auto w-full px-8 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => { setCurrentStep("upload"); setGeneratedFiles([]); setExecutionOutput(""); setResults(null); setProgress(null); setPreloadedTesters([]); }}
              className="flex items-center gap-4 cursor-pointer hover:opacity-80 transition-opacity"
            >
              <div className="w-10 h-10 bg-indigo-600 flex items-center justify-center rounded-none shadow-lg shadow-indigo-500/20">
                <Cpu className="text-primary-foreground w-6 h-6" />
              </div>
              <h1 className="text-2xl font-black text-foreground tracking-widest font-heading uppercase">
                Autograder
              </h1>
            </button>
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
        {generateMutation.isPending && (() => {
          const estimatedTotal = 5;
          const progressPct = Math.min((filesDetected / estimatedTotal) * 100, 95);

          return (
          <div className="fixed inset-0 bg-background/95 backdrop-blur-md z-50 flex items-center justify-center">
            <div className="max-w-md w-full px-8 space-y-8">
              {/* Header */}
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <div className="w-2 h-2 bg-indigo-500 rounded-full animate-pulse" />
                  <span className="text-xs font-bold text-muted-foreground uppercase tracking-[0.2em]">
                    {generationStatus}
                  </span>
                </div>
                {/* Progress bar */}
                <div className="w-full h-1.5 bg-muted rounded-full overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-indigo-500 to-violet-500 transition-all duration-1000 ease-out rounded-full"
                    style={{ width: `${Math.max(progressPct, filesDetected > 0 ? 10 : 3)}%` }}
                  />
                </div>
                <div className="flex justify-between text-[10px] text-muted-foreground font-mono">
                  <span>{filesDetected} / ~{estimatedTotal} files</span>
                  <span>~5 min</span>
                </div>
              </div>

              {/* AI Thinking — the showcase */}
              {thinkingText && (
                <div className="p-5 bg-card border border-border rounded-lg shadow-lg animate-in fade-in slide-in-from-bottom-3 duration-500">
                  <div className="flex items-center gap-2 mb-3 text-indigo-600 dark:text-indigo-400">
                    <Cpu className="w-4 h-4" />
                    <span className="text-xs font-black uppercase tracking-widest">AI Reasoning</span>
                  </div>
                  <p className="text-sm text-muted-foreground leading-relaxed italic">
                    {thinkingText}
                  </p>
                </div>
              )}
            </div>
          </div>
          );
        })()}

        <main className="min-h-[40vh]">
          {/* 1. Upload Step */}
          {currentStep === "upload" && !generateMutation.isPending && (
            <div className="space-y-12">
              <div className="mb-12">
                <h2 className="text-3xl font-black text-foreground tracking-tight font-heading uppercase">
                  Assignment
                </h2>
                <p className="text-muted-foreground mt-2 font-medium">
                  Upload files to begin grading.
                </p>
              </div>
              <UploadZone
                preloadedTesterFiles={preloadedTesters}
                preloadedTemplateFiles={preloadedTemplates}
                onFilesSelected={(data) => {
                  if (data.mode === "generate" && data.questionPaper) {
                    setUploadMode("ai");
                    if (data.templateFiles) {
                      setPreloadedTemplates(data.templateFiles);
                    }
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
              <div className="bg-amber-500/10 border border-amber-500/30 p-4 rounded-none flex flex-col sm:flex-row sm:items-center gap-4">
                <p className="text-xs text-amber-700 dark:text-amber-400 font-medium flex-1">
                  <span className="font-black uppercase tracking-wider">⚠ Disclaimer:</span>{" "}
                  AI-generated test files may not always be accurate. The number of test cases generated per question may be more or fewer than the actual mark allocation in the exam paper. Faculty should review and manually edit the tester files to ensure correctness before using them for final grading.
                </p>
                <button
                  onClick={async () => {
                    const zip = new JSZip();
                    generatedFiles.forEach((file) => {
                      zip.file(file.filename, file.code);
                    });
                    const content = await zip.generateAsync({ type: "blob" });
                    const url = URL.createObjectURL(content);
                    const a = document.createElement("a");
                    a.href = url;
                    a.download = "tester_files.zip";
                    a.click();
                    URL.revokeObjectURL(url);
                  }}
                  className="flex items-center justify-center gap-2 py-2.5 px-5 border-2 border-amber-500/30 bg-card hover:bg-amber-500/10 hover:border-amber-500 transition-all rounded-none whitespace-nowrap shrink-0 group"
                >
                  <Download className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                  <span className="text-xs font-black text-amber-700 dark:text-amber-400 uppercase tracking-widest">
                    Download Files
                  </span>
                </button>
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

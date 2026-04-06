"use client";

import React, { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { FileText, FolderOpen, Loader2 } from "lucide-react";

let pdfjsLib: any = null;

interface UploadZoneProps {
  onFilesSelected: (data: {
    mode: "generate" | "direct";
    questionPaper?: string;
    templateStructure?: string;
    submissionFiles?: File[];
    testerFiles?: File[];
    templateFiles?: File[];
  }) => void;
}

export function UploadZone({ onFilesSelected }: UploadZoneProps) {
  const [mode, setMode] = useState<"generate" | "direct">("direct");
  const [questionPaper, setQuestionPaper] = useState("");
  const [paperFile, setPaperFile] = useState<File | null>(null);
  const [templateFiles, setTemplateFiles] = useState<FileList | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Direct mode state
  const [submissionFiles, setSubmissionFiles] = useState<File[]>([]);
  const [testerFiles, setTesterFiles] = useState<File[]>([]);
  const [directTemplateFiles, setDirectTemplateFiles] = useState<File[]>([]);

  useEffect(() => {
    const initPdf = async () => {
      try {
        const { getDocument, GlobalWorkerOptions, version } = await import("pdfjs-dist");
        GlobalWorkerOptions.workerSrc = `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${version}/pdf.worker.min.mjs`;
        pdfjsLib = { getDocument };
      } catch (e) {
        console.error("Failed to load PDF.js", e);
      }
    };
    initPdf();
  }, []);

  const extractPdfText = async (file: File): Promise<string> => {
    if (!pdfjsLib) return "PDF parser not ready.";
    try {
      const arrayBuffer = await file.arrayBuffer();
      const pdf = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
      let text = "";
      for (let i = 1; i <= pdf.numPages; i++) {
        const page = await pdf.getPage(i);
        const content = await page.getTextContent();
        text += content.items.map((item: any) => item.str).join(" ") + "\n";
      }
      return text;
    } catch {
      return "Error parsing PDF.";
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setPaperFile(file);
    setQuestionPaper(file.name.endsWith(".pdf") ? await extractPdfText(file) : await file.text());
  };

  const handleSubmit = async () => {
    setIsLoading(true);
    try {
      if (mode === "direct") {
        onFilesSelected({
          mode: "direct",
          submissionFiles,
          testerFiles,
          templateFiles: directTemplateFiles,
        });
        return;
      }
      if (!templateFiles) return;
      const ignore = [
        "node_modules",
        ".git",
        "target",
        "build",
        ".settings",
        ".vscode",
        ".idea",
        "bin",
        "obj",
      ];
      const folderStructure = Array.from(templateFiles)
        .filter((f) => !ignore.some((p) => (f.webkitRelativePath || f.name).includes(`/${p}/`)))
        .map((f) => f.webkitRelativePath || f.name)
        .join("\n");
      onFilesSelected({ mode: "generate", questionPaper, templateStructure: folderStructure });
    } finally {
      setIsLoading(false);
    }
  };

  const isReady =
    mode === "generate"
      ? questionPaper.trim().length > 0 && templateFiles !== null
      : submissionFiles.length > 0 &&
        testerFiles.some((f) => f.name.endsWith(".java")) &&
        directTemplateFiles.length > 0;

  const ModeToggle = () => (
    <div className="flex bg-muted p-1 rounded-none border border-border">
      {(["direct", "generate"] as const).map((m) => (
        <button
          key={m}
          onClick={() => setMode(m)}
          className={`px-3 py-1 text-[10px] font-black transition-all rounded-none ${mode === m ? "bg-indigo-600 text-white shadow-lg" : "text-muted-foreground hover:text-foreground"}`}
        >
          {m === "direct" ? "DIRECT" : "GENERATE"}
        </button>
      ))}
    </div>
  );

  const FolderZone = ({
    label,
    hint,
    count,
    onFiles,
    multiple,
    accept,
  }: {
    label: string;
    hint: string;
    count: number;
    onFiles: (files: File[]) => void;
    multiple?: boolean;
    accept?: string;
  }) => (
    <label className="flex flex-col items-center justify-center gap-3 border-2 border-dashed border-border rounded-none p-6 cursor-pointer hover:border-indigo-500 hover:bg-indigo-500/5 transition-all text-center">
      <FolderOpen className="w-8 h-8 text-muted-foreground" />
      <div>
        <div className="text-xs font-black text-foreground uppercase tracking-widest">
          {count > 0 ? `${count} file${count !== 1 ? "s" : ""} selected` : label}
        </div>
        <div className="text-[10px] text-muted-foreground mt-0.5">{hint}</div>
      </div>
      <input
        type="file"
        className="hidden"
        multiple={multiple}
        accept={accept}
        {...(!accept
          ? ({ webkitdirectory: "", directory: "" } as React.InputHTMLAttributes<HTMLInputElement>)
          : {})}
        onChange={(e) => {
          if (e.target.files) onFiles(Array.from(e.target.files));
        }}
      />
    </label>
  );

  if (mode === "direct") {
    return (
      <div className="w-full max-w-7xl space-y-6">
        <div className="flex justify-end">
          <ModeToggle />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card className="border-border bg-card/50 backdrop-blur-xl p-6 rounded-none border-2 space-y-3">
            <div>
              <h3 className="text-sm font-black text-foreground uppercase tracking-widest">
                Submissions
              </h3>
              <p className="text-[10px] text-muted-foreground mt-0.5">
                Student .zip files (one per student)
              </p>
            </div>
            <FolderZone
              label="Select student zips"
              hint="Multiple .zip accepted"
              count={submissionFiles.length}
              onFiles={setSubmissionFiles}
              multiple
              accept=".zip"
            />
          </Card>
          <Card className="border-border bg-card/50 backdrop-blur-xl p-6 rounded-none border-2 space-y-3">
            <div>
              <h3 className="text-sm font-black text-foreground uppercase tracking-widest">
                Tester Files
              </h3>
              <p className="text-[10px] text-muted-foreground mt-0.5">
                Tester-Files folder (e.g. Q1aTester.java)
              </p>
            </div>
            <FolderZone
              label="Select Tester-Files folder"
              hint="Folder containing *Tester.java files"
              count={testerFiles.filter((f) => f.name.endsWith(".java")).length}
              onFiles={setTesterFiles}
            />
          </Card>
          <Card className="border-border bg-card/50 backdrop-blur-xl p-6 rounded-none border-2 space-y-3">
            <div>
              <h3 className="text-sm font-black text-foreground uppercase tracking-widest">
                Exam Template
              </h3>
              <p className="text-[10px] text-muted-foreground mt-0.5">
                RenameToYourUsername folder (Q1/, Q2/, Q3/)
              </p>
            </div>
            <FolderZone
              label="Select template folder"
              hint="Folder containing Q1/, Q2/, Q3/ subfolders"
              count={directTemplateFiles.length}
              onFiles={setDirectTemplateFiles}
            />
          </Card>
        </div>
        <Button
          disabled={!isReady || isLoading}
          onClick={handleSubmit}
          className="w-full h-14 rounded-none bg-indigo-600 hover:bg-indigo-500 text-white font-black uppercase tracking-[0.2em] text-xs shadow-2xl shadow-indigo-500/20 disabled:opacity-30 disabled:grayscale"
        >
          {isLoading ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : (
            <span>Upload &amp; Prepare</span>
          )}
        </Button>
      </div>
    );
  }

  // Generate mode
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 w-full max-w-7xl">
      <Card className="border-border bg-card/50 backdrop-blur-xl p-8 relative overflow-hidden flex flex-col min-h-[460px] rounded-none border-2">
        <div className="absolute top-0 right-0 p-4 z-20">
          <ModeToggle />
        </div>
        <div className="flex-1 flex flex-col pt-2">
          <div className="mb-6">
            <h3 className="text-xl font-black text-foreground font-heading tracking-tight uppercase">
              Assignment
            </h3>
            <p className="text-muted-foreground text-xs font-bold uppercase tracking-widest mt-1">
              Upload PDF or paste text
            </p>
          </div>
          <div className="flex-1 border-2 border-border rounded-none flex flex-col items-center justify-center p-6 bg-muted/20 hover:border-zinc-600 dark:hover:border-zinc-400 transition-all overflow-hidden">
            <div className="w-full h-full flex flex-col">
              <textarea
                value={questionPaper}
                onChange={(e) => setQuestionPaper(e.target.value)}
                placeholder="Paste context or drag PDF here..."
                className="w-full flex-1 bg-transparent border-none focus:ring-0 text-zinc-400 font-mono text-xs resize-none placeholder:text-zinc-700"
              />
              <div className="h-px bg-zinc-800/50 w-full my-4" />
              <label className="flex items-center justify-center gap-3 py-3 px-6 bg-secondary border border-border rounded-none cursor-pointer hover:bg-muted transition-all w-fit mx-auto group/btn">
                <FileText className="w-4 h-4 text-muted-foreground group-hover/btn:text-indigo-600 dark:group-hover/btn:text-indigo-400 transition-colors" />
                <span className="text-[10px] font-black text-muted-foreground group-hover/btn:text-foreground uppercase tracking-widest">
                  {paperFile ? paperFile.name : "Upload File"}
                </span>
                <input
                  type="file"
                  className="hidden"
                  accept=".pdf,.txt,.md"
                  onChange={handleFileUpload}
                />
              </label>
            </div>
          </div>
        </div>
      </Card>

      <Card className="border-border bg-card/50 backdrop-blur-xl p-8 relative overflow-hidden flex flex-col min-h-[460px] rounded-none border-2">
        <div className="flex-1 flex flex-col">
          <div className="mb-6">
            <h3 className="text-xl font-black text-foreground font-heading tracking-tight uppercase">
              Template
            </h3>
            <p className="text-muted-foreground text-xs font-bold uppercase tracking-widest mt-1">
              Select project structure
            </p>
          </div>
          <div className="flex-1 border-2 border-border rounded-none flex flex-col items-center justify-center p-6 bg-muted/20 hover:border-zinc-600 dark:hover:border-zinc-400 transition-all">
            <div className="flex flex-col items-center gap-6 py-8">
              <div className="p-5 bg-secondary border border-border rounded-none shadow-2xl">
                <FolderOpen className="w-10 h-10 text-indigo-600 dark:text-indigo-400" />
              </div>
              <div className="text-center">
                <div className="text-sm font-black text-foreground uppercase tracking-widest mb-1">
                  {templateFiles ? `${templateFiles.length} Files Linked` : "Select Folder"}
                </div>
                <div className="text-[10px] text-muted-foreground font-bold uppercase tracking-widest">
                  RenameToYourUsername
                </div>
              </div>
              <label className="flex items-center gap-3 px-8 py-3 bg-secondary border border-border rounded-none cursor-pointer hover:bg-muted transition-all shadow-xl active:scale-95 group/btn">
                <FolderOpen className="w-4 h-4 text-muted-foreground group-hover/btn:text-foreground" />
                <span className="text-[10px] font-black text-muted-foreground group-hover/btn:text-foreground uppercase tracking-widest">
                  Choose Folder
                </span>
                {/* @ts-ignore */}
                <input
                  type="file"
                  className="hidden"
                  webkitdirectory=""
                  directory=""
                  multiple
                  onChange={(e) => setTemplateFiles(e.target.files)}
                />
              </label>
            </div>
          </div>
          <div className="mt-8">
            <Button
              disabled={!isReady || isLoading}
              onClick={handleSubmit}
              className="w-full h-14 rounded-none bg-indigo-600 hover:bg-indigo-500 text-white font-black uppercase tracking-[0.2em] text-xs shadow-2xl shadow-indigo-500/20 active:scale-98 transition-all disabled:opacity-30 disabled:grayscale"
            >
              {isLoading ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                <span>Start Autograder</span>
              )}
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}

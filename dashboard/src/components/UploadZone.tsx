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
  preloadedTesterFiles?: File[];
  preloadedTemplateFiles?: File[];
}

export function UploadZone({ onFilesSelected, preloadedTesterFiles, preloadedTemplateFiles }: UploadZoneProps) {
  const [mode, setMode] = useState<"generate" | "direct">("direct");
  const [questionPaper, setQuestionPaper] = useState("");
  const [paperFile, setPaperFile] = useState<File | null>(null);
  const [templateFiles, setTemplateFiles] = useState<FileList | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Direct mode state
  const [submissionFiles, setSubmissionFiles] = useState<File[]>([]);
  const [testerFiles, setTesterFiles] = useState<File[]>([]);
  const [directTemplateFiles, setDirectTemplateFiles] = useState<File[]>([]);

  // Pre-populate tester files from AI generation flow
  useEffect(() => {
    if (preloadedTesterFiles && preloadedTesterFiles.length > 0) {
      setTesterFiles(preloadedTesterFiles);
      setMode("direct");
    }
  }, [preloadedTesterFiles]);

  // Pre-populate template files from AI generation flow
  useEffect(() => {
    if (preloadedTemplateFiles && preloadedTemplateFiles.length > 0) {
      setDirectTemplateFiles(preloadedTemplateFiles);
    }
  }, [preloadedTemplateFiles]);

  useEffect(() => {
    const initPdf = async () => {
      try {
        const pdfjs = await import("pdfjs-dist");
        pdfjs.GlobalWorkerOptions.workerSrc = `/pdf.worker.min.mjs`;
        pdfjsLib = { getDocument: pdfjs.getDocument };
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
    } catch (e) {
      console.error("PDF parsing error:", e);
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
      const filteredFiles = Array.from(templateFiles)
        .filter((f) => !ignore.some((p) => (f.webkitRelativePath || f.name).includes(`/${p}/`)))
        .filter((f) => !f.name.startsWith("."));

      // Read actual file contents so the AI can see method signatures and class structure
      const entries = await Promise.all(
        filteredFiles.map(async (f) => {
          const path = f.webkitRelativePath || f.name;
          if (f.name.endsWith(".java") || f.name.endsWith(".txt") || f.name.endsWith(".csv")) {
            const content = await f.text();
            return `--- FILE: ${path} ---\n${content}`;
          }
          if (f.name.endsWith(".class")) {
            return `--- FILE: ${path} (pre-compiled class) ---`;
          }
          return null;
        })
      );
      const folderStructure = (entries.filter(Boolean) as string[]).join("\n\n");
      onFilesSelected({ 
        mode: "generate", 
        questionPaper, 
        templateStructure: folderStructure,
        templateFiles: Array.from(templateFiles) 
      });
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
    countLabel,
    onFiles,
    multiple,
    accept,
  }: {
    label: string;
    hint: string;
    count: number;
    countLabel?: string;
    onFiles: (files: File[]) => void;
    multiple?: boolean;
    accept?: string;
  }) => (
    <label className="flex flex-col items-center justify-center gap-3 border-2 border-dashed border-border rounded-none p-6 cursor-pointer hover:border-indigo-500 hover:bg-indigo-500/5 transition-all text-center">
      <FolderOpen className="w-8 h-8 text-muted-foreground" />
      <div>
        <div className="text-xs font-black text-foreground uppercase tracking-widest">
          {count > 0 ? `${count} ${countLabel || (count !== 1 ? "files" : "file")} selected` : label}
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
          <Card className={`backdrop-blur-xl p-6 rounded-none border-2 space-y-3 transition-colors ${submissionFiles.length > 0 ? "border-emerald-500/40 bg-emerald-500/5" : "border-border bg-card/50"}`}>
            <div>
              <h3 className="text-sm font-black text-foreground uppercase tracking-widest">
                Submissions
              </h3>
              <p className="text-[10px] text-muted-foreground mt-0.5">
                Select folder containing student .zip files
              </p>
            </div>
            <FolderZone
              label="Select submissions folder"
              hint="Folder with student .zip files"
              count={submissionFiles.length}
              onFiles={(files) => setSubmissionFiles(files.filter((f) => f.name.endsWith(".zip")))}
            />
          </Card>
          <Card className={`backdrop-blur-xl p-6 rounded-none border-2 space-y-3 transition-colors ${testerFiles.some((f) => f.name.endsWith(".java")) ? "border-emerald-500/40 bg-emerald-500/5" : "border-border bg-card/50"}`}>
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
          <Card className={`backdrop-blur-xl p-6 rounded-none border-2 space-y-3 transition-colors ${directTemplateFiles.length > 0 ? "border-emerald-500/40 bg-emerald-500/5" : "border-border bg-card/50"}`}>
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
              count={new Set(directTemplateFiles.map((f) => ((f as any).webkitRelativePath || f.name).split("/").find((seg: string) => /^Q\d+$/i.test(seg))).filter(Boolean)).size}
              countLabel="folders"
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

  const templateFolderCount = templateFiles
    ? new Set(Array.from(templateFiles).map((f) => (f.webkitRelativePath || f.name).split("/").find((seg) => /^Q\d+$/i.test(seg))).filter(Boolean)).size
    : 0;

  return (
    <div className="w-full max-w-7xl space-y-6">
      <div className="flex justify-end">
        <ModeToggle />
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card className={`backdrop-blur-xl p-6 rounded-none border-2 space-y-3 transition-colors ${questionPaper.trim() ? "border-emerald-500/40 bg-emerald-500/5" : "border-border bg-card/50"}`}>
          <div>
            <h3 className="text-sm font-black text-foreground uppercase tracking-widest">
              Assignment
            </h3>
            <p className="text-[10px] text-muted-foreground mt-0.5">
              Upload question paper PDF
            </p>
          </div>
          <label className="flex flex-col items-center justify-center gap-3 border-2 border-dashed border-border rounded-none p-6 cursor-pointer hover:border-indigo-500 hover:bg-indigo-500/5 transition-all text-center">
            <FileText className="w-8 h-8 text-muted-foreground" />
            <div>
              <div className="text-xs font-black text-foreground uppercase tracking-widest">
                {paperFile ? paperFile.name : "Select question paper"}
              </div>
              <div className="text-[10px] text-muted-foreground mt-0.5">PDF, TXT, or MD file</div>
            </div>
            <input
              type="file"
              className="hidden"
              accept=".pdf,.txt,.md"
              onChange={handleFileUpload}
            />
          </label>
        </Card>

        <Card className={`backdrop-blur-xl p-6 rounded-none border-2 space-y-3 transition-colors ${templateFolderCount > 0 ? "border-emerald-500/40 bg-emerald-500/5" : "border-border bg-card/50"}`}>
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
            count={templateFolderCount}
            countLabel="folders"
            onFiles={(files) => {
              const dt = new DataTransfer();
              files.forEach((f) => dt.items.add(f));
              setTemplateFiles(dt.files);
            }}
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
          <span>Generate Tests</span>
        )}
      </Button>
    </div>
  );
}

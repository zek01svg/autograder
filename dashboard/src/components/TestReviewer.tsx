"use client";

import React, { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";
import { Check, X, Save, RefreshCw, FileCode, Loader2 } from "lucide-react";
import { TestFile } from "@/lib/schema";
import { ScrollArea } from "@/components/ui/scroll-area";

interface TestReviewerProps {
  files: TestFile[];
  onSave: (approvedFiles: TestFile[]) => void;
  isLoading: boolean;
}

export function TestReviewer({ files, onSave, isLoading }: TestReviewerProps) {
  const handleSave = () => {
    onSave(files); // Save all files directly
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-black text-foreground flex items-center gap-4 font-heading tracking-tight">
          <FileCode className="w-8 h-8 text-indigo-600 dark:text-indigo-400" />
          Validation & Inspection
        </h2>
        <div className="flex gap-2">
          <Badge
            variant="outline"
            className="text-muted-foreground border-border bg-muted/50 rounded-none"
          >
            {files.length} JUnit Files Ready
          </Badge>
          <Button
            onClick={handleSave}
            disabled={isLoading}
            className="bg-indigo-600 hover:bg-indigo-500 text-white h-12 px-8 font-black shadow-lg shadow-indigo-500/20 uppercase tracking-widest text-xs rounded-none transition-all hover:scale-105 active:scale-95"
          >
            {isLoading ? (
              <Loader2 className="w-4 h-4 mr-3 animate-spin" />
            ) : (
              <Check className="w-4 h-4 mr-3" />
            )}
            {isLoading ? "Persisting..." : "Commit All Tests"}
          </Button>
        </div>
      </div>

      <div className="grid gap-6">
        {files.map((file, index) => (
          <Card
            key={index}
            className="bg-card border-border shadow-xl overflow-hidden hover:border-zinc-500 transition-colors rounded-none border-2"
          >
            <CardHeader className="flex flex-row items-center justify-between py-6 bg-muted/50">
              <div className="flex flex-col gap-1">
                <CardTitle className="text-2xl font-black flex items-center gap-4 text-indigo-600 dark:text-indigo-400 font-heading tracking-tight">
                  {file.filename}
                  <Badge
                    variant="outline"
                    className="text-[10px] border-indigo-500/40 text-indigo-600/80 dark:text-indigo-400/80 uppercase tracking-[0.2em] font-black px-3"
                  >
                    {file.questionRef}
                  </Badge>
                </CardTitle>
                <p className="text-sm text-muted-foreground font-medium leading-relaxed max-w-3xl mt-1 italic">
                  {file.explanation}
                </p>
              </div>
            </CardHeader>
            <CardContent className="p-0 border-t border-border">
              <ScrollArea className="h-[300px] w-full rounded-none">
                <SyntaxHighlighter
                  language="java"
                  style={vscDarkPlus}
                  showLineNumbers={true}
                  wrapLines={true}
                  lineProps={{
                    style: { display: "block", width: "100%" },
                  }}
                  customStyle={{
                    margin: 0,
                    padding: "1.5rem",
                    fontSize: "0.9rem",
                    backgroundColor: "transparent",
                    lineHeight: "1.7",
                    minHeight: "400px",
                    width: "100%",
                    display: "flex",
                    flexDirection: "column",
                  }}
                  codeTagProps={{
                    style: {
                      fontFamily: "var(--font-geist-mono)",
                      whiteSpace: "pre-wrap",
                      wordBreak: "break-all",
                      display: "block",
                      width: "100%",
                    },
                  }}
                >
                  {file.code}
                </SyntaxHighlighter>
              </ScrollArea>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

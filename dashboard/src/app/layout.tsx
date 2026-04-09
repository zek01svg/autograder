import type { Metadata } from "next";
import { Geist, Geist_Mono, Outfit } from "next/font/google"; // Import Outfit for headings
import "./globals.css";

const geistSans = Geist({
  variable: "--font-sans", // Standardized mapping
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-mono", // Standardized mapping
  subsets: ["latin"],
});

const outfit = Outfit({
  variable: "--font-heading",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "IS442 — Instructor AI Dashboard",
  description: "Advanced assignment grading and test generation",
};

import { Providers } from "@/components/Providers";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} ${outfit.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col bg-background text-foreground selection:bg-indigo-500/30 selection:text-indigo-200">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}

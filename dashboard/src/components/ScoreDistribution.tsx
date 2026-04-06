"use client";

import React, { useMemo } from "react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
  Bar,
  ComposedChart,
  Cell,
  Legend,
} from "recharts";
import { calculateStats, generateBellCurve } from "@/lib/stats";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface Student {
  username: string;
  score: number;
  maxScore: number;
}

interface ScoreDistributionProps {
  students: Student[];
}

export function ScoreDistribution({ students }: ScoreDistributionProps) {
  const stats = useMemo(() => {
    const scores = students.map((s) => s.score);
    const maxPossible = students[0]?.maxScore || 20;
    return calculateStats(scores, maxPossible);
  }, [students]);

  const chartData = useMemo(() => {
    // 1. Generate Bell Curve Data
    const { mean, stdDev, min, max } = stats;
    const curvePoints = generateBellCurve(mean, stdDev, min, max);

    // 2. Map Histogram Frequencies onto the same X-axis
    // Create bins for scores (integer increments)
    const bins: Record<number, number> = {};
    students.forEach((s) => {
      const b = Math.floor(s.score);
      bins[b] = (bins[b] || 0) + 1;
    });

    // Normalize histogram to fit chart height (0 to max PDF value)
    const maxFreq = Math.max(...Object.values(bins), 1);
    const maxPdf = Math.max(...curvePoints.map((p) => p.y), 0.1);
    const scale = (maxPdf * 0.8) / maxFreq;

    return curvePoints.map((p) => {
      const roundedX = Math.floor(p.x);
      return {
        ...p,
        frequency: (bins[roundedX] || 0) * scale,
        rawFrequency: bins[roundedX] || 0,
      };
    });
  }, [students, stats]);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <MetricCard
          title="Average"
          value={stats.mean.toFixed(1)}
          sub={`/ ${students[0]?.maxScore || 20}`}
        />
        <MetricCard title="Median" value={stats.median.toFixed(1)} />
        <MetricCard title="Variance" value={stats.stdDev.toFixed(2)} />
        <MetricCard
          title="Pass Rate"
          value={`${stats.passRate.toFixed(0)}%`}
          color="text-green-600 dark:text-green-400"
        />
      </div>

      <Card className="bg-card border-border shadow-2xl p-6 overflow-hidden border-2 rounded-none">
        <CardHeader className="px-0 pt-0 pb-10">
          <CardTitle className="text-xl font-black text-foreground flex items-center justify-between font-heading uppercase">
            Distribution
            <span className="text-[10px] text-muted-foreground font-bold uppercase tracking-widest">
              Normal PDF
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0 h-[400px]">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="colorCurve" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid
                strokeDasharray="3 3"
                stroke="currentColor"
                className="text-muted-foreground/20"
                vertical={false}
              />
              <XAxis
                dataKey="x"
                type="number"
                domain={[0, students[0]?.maxScore || 20]}
                stroke="currentColor"
                className="text-muted-foreground"
                fontSize={12}
                tickLine={false}
                axisLine={false}
              />
              <YAxis hide />
              <Tooltip
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const data = payload[0].payload;
                    return (
                      <div className="bg-popover border border-border p-3 rounded-none shadow-2xl">
                        <p className="text-muted-foreground text-[10px] font-bold uppercase mb-1">
                          Score: {data.x}
                        </p>
                        <p className="text-indigo-600 dark:text-indigo-400 font-bold text-lg">
                          {data.rawFrequency} Students
                        </p>
                      </div>
                    );
                  }
                  return null;
                }}
              />
              <Bar
                dataKey="frequency"
                barSize={12}
                fill="currentColor"
                className="text-muted-foreground/30"
                radius={0}
                opacity={0.5}
              />
              <Area
                type="monotone"
                dataKey="y"
                stroke="#6366f1"
                strokeWidth={3}
                fillOpacity={1}
                fill="url(#colorCurve)"
                animationDuration={2000}
              />
              <ReferenceLine
                x={stats.mean}
                stroke="#B45309"
                strokeDasharray="5 5"
                label={{
                  position: "top",
                  value: "Avg",
                  fill: "#B45309",
                  fontSize: 10,
                  fontWeight: 900,
                }}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    </div>
  );
}

function MetricCard({
  title,
  value,
  sub,
  color = "text-indigo-600 dark:text-indigo-400",
}: {
  title: string;
  value: string;
  sub?: string;
  color?: string;
}) {
  return (
    <Card className="bg-card border-border border-2 rounded-none">
      <CardContent className="p-4 flex flex-col gap-1 text-center">
        <p className="text-[10px] text-muted-foreground uppercase tracking-widest font-black leading-none mb-2">
          {title}
        </p>
        <div className="flex items-baseline justify-center gap-1">
          <span className={`text-2xl font-black ${color}`}>{value}</span>
          {sub && <span className="text-xs text-muted-foreground/60 font-medium">{sub}</span>}
        </div>
      </CardContent>
    </Card>
  );
}

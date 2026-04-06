/**
 * Statistical utilities for student score analysis.
 */

export interface StatSummary {
  mean: number;
  median: number;
  stdDev: number;
  min: number;
  max: number;
  passCount: number;
  passRate: number;
}

export function calculateStats(scores: number[], maxPossibleScore: number): StatSummary {
  if (scores.length === 0) {
    return { mean: 0, median: 0, stdDev: 0, min: 0, max: 0, passCount: 0, passRate: 0 };
  }

  const sorted = [...scores].sort((a, b) => a - b);
  const sum = scores.reduce((a, b) => a + b, 0);
  const mean = sum / scores.length;

  // Median
  const mid = Math.floor(sorted.length / 2);
  const median = sorted.length % 2 !== 0 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;

  // Standard Deviation
  const variance =
    scores.reduce((acc, score) => acc + Math.pow(score - mean, 2), 0) / scores.length;
  const stdDev = Math.sqrt(variance);

  // Pass Rate (Assuming 50% is pass)
  const passCount = scores.filter((s) => s >= maxPossibleScore / 2).length;
  const passRate = (passCount / scores.length) * 100;

  return {
    mean,
    median,
    stdDev,
    min: sorted[0],
    max: sorted[sorted.length - 1],
    passCount,
    passRate,
  };
}

/**
 * Normal Probability Density Function
 */
export function normalPDF(x: number, mean: number, stdDev: number): number {
  if (stdDev === 0) return x === mean ? 1 : 0;
  return (
    (1 / (stdDev * Math.sqrt(2 * Math.PI))) * Math.exp(-0.5 * Math.pow((x - mean) / stdDev, 2))
  );
}

/**
 * Generates data points for the bell curve
 */
export function generateBellCurve(mean: number, stdDev: number, min: number, max: number) {
  const points = [];
  const range = max - min;
  const step = range / 40; // 40 points for a smooth curve

  // Add some padding to the curve
  const start = Math.max(0, min - (stdDev || 1));
  const end = max + (stdDev || 1);
  const actualStep = (end - start) / 50;

  for (let x = start; x <= end; x += actualStep) {
    points.push({
      x: Number(x.toFixed(2)),
      y: normalPDF(x, mean, stdDev),
    });
  }

  return points;
}

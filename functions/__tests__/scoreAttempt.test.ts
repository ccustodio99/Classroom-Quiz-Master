import { applyScoringMode, computeAccuracy, computePoints, SubmissionUpdate } from "../src/index";

describe("computePoints", () => {
  it("returns zero when incorrect", () => {
    expect(computePoints(false, 30000, 1000)).toBe(0);
  });

  it("caps score at 1000", () => {
    expect(computePoints(true, 1000, 0)).toBe(1000);
  });

  it("applies linear scaling with time left", () => {
    const points = computePoints(true, 60000, 30000);
    expect(points).toBe(700);
  });
});

describe("computeAccuracy", () => {
  it("handles zero total safely", () => {
    expect(computeAccuracy(5, 0)).toBe(0);
  });

  it("rounds to nearest integer", () => {
    expect(computeAccuracy(7, 9)).toBe(78);
  });
});

describe("applyScoringMode", () => {
  const base: SubmissionUpdate = {
    attempts: 1,
    bestScore: 800,
    lastScore: 650,
    totalScore: 800,
    averageScore: 800,
    effectiveScore: 800,
  };

  it("keeps best score when using best mode", () => {
    const update = applyScoringMode("best", base, 600);
    expect(update.attempts).toBe(2);
    expect(update.bestScore).toBe(800);
    expect(update.effectiveScore).toBe(800);
  });

  it("uses last score when scoring mode is last", () => {
    const update = applyScoringMode("last", base, 500);
    expect(update.lastScore).toBe(500);
    expect(update.effectiveScore).toBe(500);
  });

  it("computes rounded average for avg mode", () => {
    const update = applyScoringMode("avg", base, 700);
    // total score becomes 1500 -> average 750
    expect(update.totalScore).toBe(1500);
    expect(update.averageScore).toBe(750);
    expect(update.effectiveScore).toBe(750);
  });
});

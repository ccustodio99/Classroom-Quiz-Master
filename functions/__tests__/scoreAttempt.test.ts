import { computePoints, computeAccuracy } from "../src/index";

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

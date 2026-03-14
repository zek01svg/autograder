package grader.report;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe, single-line console progress bar.
 *
 * Usage:
 * ProgressBar bar = new ProgressBar("Grading", total);
 * // from any thread:
 * bar.advance(); // increments and redraws
 * bar.finish(); // prints the final state with a newline
 */
public class ProgressBar {

  private static final int BAR_WIDTH = 30;

  private final String label;
  private final int total;
  private final AtomicInteger completed = new AtomicInteger(0);

  public ProgressBar(String label, int total) {
    this.label = label;
    this.total = total;
    render(0);
  }

  /** Thread-safe: increment progress by 1 and redraw. */
  public void advance() {
    int done = completed.incrementAndGet();
    render(done);
  }

  /** Print the final 100 % state followed by a newline. */
  public void finish() {
    render(total);
    System.out.println();
  }

  private void render(int done) {
    int filled = (total == 0) ? BAR_WIDTH : (int) ((double) done / total * BAR_WIDTH);
    int empty = BAR_WIDTH - filled;
    int pct = (total == 0) ? 100 : (int) ((double) done / total * 100);

    StringBuilder sb = new StringBuilder("\r");
    sb.append(label).append(": [");
    for (int i = 0; i < filled; i++)
      sb.append('\u2588'); // █
    for (int i = 0; i < empty; i++)
      sb.append('\u2591'); // ░
    sb.append("] ").append(done).append("/").append(total);
    sb.append(" (").append(pct).append("%)");

    System.out.print(sb.toString());
    System.out.flush();
  }
}

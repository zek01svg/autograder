package grader.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data class representing the grading result for a single student submission.
 * Stores per-question scores (Q1a, Q1b, Q2a, Q2b, Q3) and aggregates a total.
 */
public class GradeResult {

  private String studentId;
  private boolean compiled;
  private Map<String, Double> questionScores;
  private List<String> anomalies;

  public GradeResult(String studentId, boolean compiled) {
    this.studentId = studentId;
    this.compiled = compiled;
    this.questionScores = new HashMap<>();
    this.anomalies = new ArrayList<>();
  }

  /**
   * Construct a GradeResult for per-question scoring (used by the E2E pipeline).
   */
  public GradeResult(String studentId) {
    this(studentId, true);
  }

  public void addAnomaly(String anomaly) {
    this.anomalies.add(anomaly);
  }

  public List<String> getAnomalies() {
    return anomalies;
  }

  public String getStudentId() {
    return studentId;
  }

  public boolean isCompiled() {
    return compiled;
  }

  /**
   * Set the score for a specific question (e.g., "Q1a", "Q2b", "Q3").
   */
  public void setQuestionScore(String question, double score) {
    questionScores.put(question, score);
  }

  /**
   * Get the score for a specific question. Returns 0.0 if not set.
   */
  public double getQuestionScore(String question) {
    return questionScores.getOrDefault(question, 0.0);
  }

  /**
   * Get the total score across all questions.
   */
  public double getTotalScore() {
    double total = 0;
    for (double s : questionScores.values()) {
      total += s;
    }
    return total;
  }

  @Override
  public String toString() {
    return "GradeResult{studentId='" + studentId + "', compiled=" + compiled
        + ", totalScore=" + getTotalScore() + ", questions=" + questionScores + "}";
  }
}

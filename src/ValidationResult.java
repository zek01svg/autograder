package src;

import java.util.ArrayList;

/**
 * Holds the result of validating a single student ZIP submission.
 * Anomalies are blocking issues; warnings are informational.
 */
public class ValidationResult {

  private boolean ok;
  private String studentId;
  private String extractedRoot;
  private ArrayList<String> anomalies;
  private ArrayList<String> warnings;

  public ValidationResult(String studentId) {
    this.ok = true;
    this.studentId = studentId;
    this.extractedRoot = "";
    this.anomalies = new ArrayList<>();
    this.warnings = new ArrayList<>();
  }

  // --- Getters ---

  public boolean isOk() {
    return ok;
  }

  public String getStudentId() {
    return studentId;
  }

  public String getExtractedRoot() {
    return extractedRoot;
  }

  public ArrayList<String> getAnomalies() {
    return anomalies;
  }

  public ArrayList<String> getWarnings() {
    return warnings;
  }

  // --- Setters ---

  public void setOk(boolean ok) {
    this.ok = ok;
  }

  public void setExtractedRoot(String extractedRoot) {
    this.extractedRoot = extractedRoot;
  }

  // --- Display ---

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(studentId).append(": ").append(ok ? "OK" : "FAIL").append("\n");
    if (!anomalies.isEmpty()) {
      sb.append("  Anomalies:\n");
      for (String a : anomalies) {
        sb.append("    - ").append(a).append("\n");
      }
    }
    if (!warnings.isEmpty()) {
      sb.append("  Warnings:\n");
      for (String w : warnings) {
        sb.append("    - ").append(w).append("\n");
      }
    }
    return sb.toString();
  }
}

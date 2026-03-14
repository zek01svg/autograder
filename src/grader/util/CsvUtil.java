package grader.util;

import java.io.*;
import java.util.ArrayList;

/**
 * Utility helpers for CSV file operations.
 * Reads and writes CSV files in the IS442 ScoreSheet format.
 */
public class CsvUtil {

  /**
   * Represents a single row from the IS442 ScoreSheet CSV.
   */
  public static class ScoreRow {
    public String orgDefinedId;
    public String username;
    public String lastName;
    public String firstName;
    public String email;
    public String gradeNumerator;
    public String gradeDenominator;
    public String endOfLineIndicator;

    public ScoreRow(String orgDefinedId, String username, String lastName,
        String firstName, String email, String gradeNumerator,
        String gradeDenominator, String endOfLineIndicator) {
      this.orgDefinedId = orgDefinedId;
      this.username = username;
      this.lastName = lastName;
      this.firstName = firstName;
      this.email = email;
      this.gradeNumerator = gradeNumerator;
      this.gradeDenominator = gradeDenominator;
      this.endOfLineIndicator = endOfLineIndicator;
    }

    /**
     * Extract the clean username (without # prefix).
     */
    public String cleanUsername() {
      if (username != null && username.startsWith("#")) {
        return username.substring(1);
      }
      return username;
    }

    public String toCsvLine() {
      return orgDefinedId + "," + username + "," + lastName + "," +
          firstName + "," + email + "," + gradeNumerator + "," +
          gradeDenominator + "," + endOfLineIndicator;
    }
  }

  /** CSV header line */
  public static final String HEADER = "OrgDefinedId,Username,Last Name,First Name,Email," +
      "Calculated Final Grade Numerator,Calculated Final Grade Denominator,End-of-Line Indicator";

  /**
   * Read the IS442 ScoreSheet CSV file.
   *
   * @param csvPath path to the CSV file
   * @return list of ScoreRow objects (one per student)
   */
  public static ArrayList<ScoreRow> readScoreSheet(String csvPath) throws IOException {
    ArrayList<ScoreRow> rows = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
      String line = br.readLine(); // skip header
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty())
          continue;

        String[] parts = line.split(",", -1);
        if (parts.length < 8)
          continue;

        rows.add(new ScoreRow(
            parts[0], parts[1], parts[2], parts[3],
            parts[4], parts[5], parts[6], parts[7]));
      }
    }
    return rows;
  }

  /**
   * Write the score sheet CSV to a file.
   *
   * @param outputPath path to write the CSV file
   * @param rows       the score rows to write
   */
  public static void writeScoreSheet(String outputPath, ArrayList<ScoreRow> rows) throws IOException {
    try (PrintWriter pw = new PrintWriter(new FileWriter(outputPath))) {
      pw.println(HEADER);
      for (ScoreRow row : rows) {
        pw.println(row.toCsvLine());
      }
    }
  }
}

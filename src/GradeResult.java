/**
 * Data class representing the grading result for a single submission.
 * (Stub — to be fleshed out on feature/compile-execute-grade branch.)
 */
public class GradeResult {

    private String studentId;
    private boolean compiled;
    private int score;
    private String feedback;

    public GradeResult(String studentId, boolean compiled, int score, String feedback) {
        this.studentId = studentId;
        this.compiled = compiled;
        this.score = score;
        this.feedback = feedback;
    }

    public String getStudentId() {
        return studentId;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public int getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }

    @Override
    public String toString() {
        return "GradeResult{studentId='" + studentId + "', compiled=" + compiled
                + ", score=" + score + ", feedback='" + feedback + "'}";
    }
}

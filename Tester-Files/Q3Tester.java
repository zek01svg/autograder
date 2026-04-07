import java.util.*;

// Assuming Shape, Circle, Rectangle classes are available in the classpath
// (as they are provided as .class files) and their toString() methods
// produce output in the format "Name=>Area:Perimeter" with rounded values.
public class Q3Tester extends Q3 {
    private static double score;
    private static String qn = "Q3";

    public static void main(String[] args) {
        grade();
        System.out.println(score);
    }

    // Helper method to create a string representation of a shape with the expected rounding (nearest integer, then .0 suffix)
    // This mimics the behavior observed in the student's main method sample outputs.
    private static String formatShape(String name, double area, double perimeter) {
        return String.format("%s=>%.1f:%.1f", name, (double)Math.round(area), (double)Math.round(perimeter));
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;
        score = 0; 

        // Test Case 1: Mixed shapes, some above area threshold (1000), some below.
        // Should filter out R1 and C1. Then sort R3, C2, R2 by Area Desc, then Perimeter Desc.
        {
            try {
                List<Shape> shapes = new ArrayList<>();
                shapes.add(new Rectangle("R1", 1000, 2));   // Area 2000 ( > 1000) -> filtered out
                shapes.add(new Rectangle("R2", 4, 2.5));     // Area 10, Perimeter 13 -> kept
                shapes.add(new Circle("C1", 50));            // Area ~7854 ( > 1000) -> filtered out
                shapes.add(new Rectangle("R3", 20, 30));     // Area 600, Perimeter 100 -> kept
                shapes.add(new Circle("C2", 5));             // Area ~78.5, Perimeter ~31.4 -> kept

                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, shapes);

                List<String> expectedList = new ArrayList<>();
                expectedList.add(formatShape("R3", 600.0, 100.0));
                expectedList.add(formatShape("C2", Math.PI * 5 * 5, 2 * Math.PI * 5));
                expectedList.add(formatShape("R2", 10.0, 13.0));

                String expected = expectedList.toString();
                List<Shape> actualShapes = sortShapes(shapes);
                String actual = actualShapes.toString();

                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 2: All shapes with area <= 1000, including multiple shapes with identical areas.
        // Should sort by area descending, then perimeter descending for ties.
        {
            try {
                List<Shape> shapes = new ArrayList<>();
                shapes.add(new Rectangle("RA", 50, 10)); // Area 500, Perim 120
                shapes.add(new Rectangle("RB", 25, 20)); // Area 500, Perim 90
                shapes.add(new Rectangle("RC", 100, 5)); // Area 500, Perim 210
                shapes.add(new Circle("C3", 10));       // Area ~314.159, Perim ~62.831
                shapes.add(new Rectangle("RD", 15, 15)); // Area 225, Perim 60

                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, shapes);

                // Expected order: RC (500, 210), RA (500, 120), RB (500, 90), C3 (~314, ~63), RD (225, 60)
                List<String> expectedList = new ArrayList<>();
                expectedList.add(formatShape("RC", 500.0, 210.0));
                expectedList.add(formatShape("RA", 500.0, 120.0));
                expectedList.add(formatShape("RB", 500.0, 90.0));
                expectedList.add(formatShape("C3", Math.PI * 10 * 10, 2 * Math.PI * 10));
                expectedList.add(formatShape("RD", 225.0, 60.0));

                String expected = expectedList.toString();
                List<Shape> actualShapes = sortShapes(shapes);
                String actual = actualShapes.toString();

                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 3: All shapes in the list have an area greater than 1000.
        // The resulting list should be empty after filtering.
        {
            try {
                List<Shape> shapes = new ArrayList<>();
                shapes.add(new Rectangle("R_Large1", 100, 15)); // Area 1500
                shapes.add(new Circle("C_Large1", 20));       // Area ~1256
                shapes.add(new Rectangle("R_Large2", 500, 3));  // Area 1500

                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, shapes);

                String expected = "[]"; 
                List<Shape> actualShapes = sortShapes(shapes);
                String actual = actualShapes.toString();

                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 4: Shapes with some having the same area and perimeter, testing stable sort for equal values.
        // R_Same1 and R_Same2 have identical area and perimeter. Their relative order should be preserved or be consistent.
        {
            try {
                List<Shape> shapes = new ArrayList<>();
                shapes.add(new Rectangle("R_Same1", 10, 20)); // Area 200, Perim 60
                shapes.add(new Rectangle("R_Same2", 20, 10)); // Area 200, Perim 60
                shapes.add(new Rectangle("R_Same3", 5, 40));  // Area 200, Perim 90
                
                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, shapes);

                // All <= 1000. All areas are 200.
                // Perimeters: R_Same3=90, R_Same1=60, R_Same2=60.
                // Order: R_Same3 (highest perim), then R_Same1, R_Same2 (original order for tie).
                List<String> expectedList = new ArrayList<>();
                expectedList.add(formatShape("R_Same3", 200.0, 90.0));
                expectedList.add(formatShape("R_Same1", 200.0, 60.0));
                expectedList.add(formatShape("R_Same2", 200.0, 60.0));

                String expected = expectedList.toString();
                List<Shape> actualShapes = sortShapes(shapes);
                String actual = actualShapes.toString();

                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 5: List containing only one shape, which is within the area limit.
        // Should return a list with that single shape.
        {
            try {
                List<Shape> shapes = new ArrayList<>();
                shapes.add(new Circle("C_Single", 12)); // Area ~452.38, Perim ~75.39

                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, shapes);

                List<String> expectedList = new ArrayList<>();
                expectedList.add(formatShape("C_Single", Math.PI * 12 * 12, 2 * Math.PI * 12));

                String expected = expectedList.toString();
                List<Shape> actualShapes = sortShapes(shapes);
                String actual = actualShapes.toString();

                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);
                if (expected.equals(actual)) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}

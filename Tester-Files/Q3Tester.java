import java.util.*;

// --- Mock classes for Shape, Rectangle, Circle and ShapeComparator for testing purposes ---
// These are minimal implementations to allow the tester to compile and compute expected values.
// The student's actual classes (Shape, Rectangle, Circle, DataException) are expected to be on the classpath.
// The student's ShapeComparator.java is also expected to be compiled and used by the student's Q3.java.

// Minimal Shape interface/abstract class to interact with student's code
// Assuming the methods based on problem description and student's Q3.java main method
interface IShapeForTest {
    String getName();
    double getArea();
    double getPerimeter();
    String toString(); // To match student's output format
}

class MockRectangle implements IShapeForTest {
    private String name;
    private double width;
    private double height;

    public MockRectangle(String name, double width, double height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    @Override
    public String getName() { return name; }

    @Override
    public double getArea() { return width * height; }

    @Override
    public double getPerimeter() { return 2 * (width + height); }

    @Override
    public String toString() {
        return name + ">=%s:%.1f".formatted(String.format("%.1f", getArea()), getPerimeter());
    }
}

class MockCircle implements IShapeForTest {
    private String name;
    private double radius;
    private static final double CUSTOM_PI = 3.14; // Matching problem's implied PI value

    public MockCircle(String name, double radius) {
        this.name = name;
        this.radius = radius;
    }

    @Override
    public String getName() { return name; }

    @Override
    public double getArea() { return CUSTOM_PI * radius * radius; }

    @Override
    public double getPerimeter() {
        // Based on sample output (C1=>314.0:63.0 for r=10, 2*3.14*10 = 62.8 rounded to 63)
        return (double) Math.round(2 * CUSTOM_PI * radius);
    }

    @Override
    public String toString() {
        return name + ">=%s:%.1f".formatted(String.format("%.1f", getArea()), getPerimeter());
    }
}
// --- End Mock classes ---

// Student's ShapeComparator is expected to be public and implement Comparator<Shape>
// For the tester to compile, if the actual Shape/Rectangle/Circle classes are not directly available,
// we might need to rely on the student's Q3.java being compiled *first* or have minimal interfaces/base classes here.
// Assuming student's Shape, Rectangle, Circle, and ShapeComparator are available on classpath for compilation.

public class Q3Tester extends Q3 {
    private static double score;
    private static String qn = "Q3";

    public static void main(String[] args) {
        grade();
        System.out.println(score);
    }

    // Helper method to create a formatted string from a list of IShapeForTest objects
    private static String getExpectedString(List<IShapeForTest> shapes) {
        List<String> formattedStrings = new ArrayList<>();
        for (IShapeForTest shape : shapes) {
            formattedStrings.add(shape.toString());
        }
        return formattedStrings.toString();
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;

        // Test Case 1: Mixed shapes, some above area 1000 (should be filtered), some ties in area, different perimeters
        {
            try {
                List<Shape> inputs = new ArrayList<>();
                inputs.add(new Rectangle("R1", 8, 3)); // Area 24, Perim 22
                inputs.add(new Rectangle("R2", 6, 4)); // Area 24, Perim 20
                inputs.add(new Rectangle("R3", 12, 2)); // Area 24, Perim 28
                inputs.add(new Circle("C1", 10)); // Area 314, Perim 63
                inputs.add(new Circle("C2", 2)); // Area 12.56, Perim 13
                inputs.add(new Rectangle("R4", 50, 30)); // Area 1500 (filter out)
                inputs.add(new Circle("C3", 20)); // Area 1256 (filter out)

                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, "Mixed shapes with filtering and ties");

                // Expected order: C1(314,63), R3(24,28), R1(24,22), R2(24,20), C2(12.56,13)
                List<IShapeForTest> expectedShapes = new ArrayList<>();
                expectedShapes.add(new MockCircle("C1", 10));
                expectedShapes.add(new MockRectangle("R3", 12, 2));
                expectedShapes.add(new MockRectangle("R1", 8, 3));
                expectedShapes.add(new MockRectangle("R2", 6, 4));
                expectedShapes.add(new MockCircle("C2", 2));
                String expected = getExpectedString(expectedShapes);

                List<Shape> actual = sortShapes(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);

                if (expected.equals(actual.toString())) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 2: Only rectangles, diverse areas and perimeters, all within limit
        {
            try {
                List<Shape> inputs = new ArrayList<>();
                inputs.add(new Rectangle("RectA", 10, 5)); // Area 50, Perim 30
                inputs.add(new Rectangle("RectB", 4, 10)); // Area 40, Perim 28
                inputs.add(new Rectangle("RectC", 20, 2)); // Area 40, Perim 44
                inputs.add(new Rectangle("RectD", 3, 3)); // Area 9, Perim 12

                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, "Only rectangles, diverse values");

                // Expected order: RectA(50,30), RectC(40,44), RectB(40,28), RectD(9,12)
                List<IShapeForTest> expectedShapes = new ArrayList<>();
                expectedShapes.add(new MockRectangle("RectA", 10, 5));
                expectedShapes.add(new MockRectangle("RectC", 20, 2));
                expectedShapes.add(new MockRectangle("RectB", 4, 10));
                expectedShapes.add(new MockRectangle("RectD", 3, 3));
                String expected = getExpectedString(expectedShapes);

                List<Shape> actual = sortShapes(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);

                if (expected.equals(actual.toString())) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 3: Only circles, diverse areas and perimeters
        {
            try {
                List<Shape> inputs = new ArrayList<>();
                inputs.add(new Circle("CircA", 5)); // Area 78.5, Perim 31
                inputs.add(new Circle("CircB", 8)); // Area 200.96, Perim 50
                inputs.add(new Circle("CircC", 1)); // Area 3.14, Perim 6

                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, "Only circles, diverse values");

                // Expected order: CircB(200.96,50), CircA(78.5,31), CircC(3.14,6)
                List<IShapeForTest> expectedShapes = new ArrayList<>();
                expectedShapes.add(new MockCircle("CircB", 8));
                expectedShapes.add(new MockCircle("CircA", 5));
                expectedShapes.add(new MockCircle("CircC", 1));
                String expected = getExpectedString(expectedShapes);

                List<Shape> actual = sortShapes(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);

                if (expected.equals(actual.toString())) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 4: All shapes filtered out (area > 1000)
        {
            try {
                List<Shape> inputs = new ArrayList<>();
                inputs.add(new Rectangle("RBig1", 100, 20)); // Area 2000
                inputs.add(new Circle("CBig1", 30)); // Area 2826

                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, "All shapes filtered out");
                String expected = "[]";
                List<Shape> actual = sortShapes(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);

                if (expected.equals(actual.toString())) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test Case 5: Single shape in the list
        {
            try {
                List<Shape> inputs = new ArrayList<>();
                inputs.add(new Rectangle("SingleR", 15, 10)); // Area 150, Perim 50

                System.out.printf("Test %d: sortShapes(%s)%n", tcNum++, "Single shape");

                List<IShapeForTest> expectedShapes = new ArrayList<>();
                expectedShapes.add(new MockRectangle("SingleR", 15, 10));
                String expected = getExpectedString(expectedShapes);

                List<Shape> actual = sortShapes(inputs);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", actual);

                if (expected.equals(actual.toString())) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
                e.printStackTrace();
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}

import java.util.*;

public class Q3Tester extends Q3 {
    private static double score;
    private static String qn = "Q3";

    public static void main(String[] args) {
        grade();
        System.out.println(score);
    }

    public static void grade() {
        System.out.println("-------------------------------------------------------");
        System.out.println("---------------------- " + qn + " ----------------------------");
        System.out.println("-------------------------------------------------------");
        int tcNum = 1;

        // Test 1: Sort by Area descending
        {
            try {
                ArrayList<Shape> shapes = new ArrayList<>();
                shapes.add(new Rectangle("R1", 10, 2)); // Area 20, Perim 24
                shapes.add(new Rectangle("R2", 4, 2.5)); // Area 10, Perim 13
                shapes.add(new Rectangle("R3", 5, 6)); // Area 30, Perim 22
                System.out.printf("Test %d: sortShapes(R1, R2, R3)%n", tcNum++);
                String expected = "[R3=>30.0:22.0, R1=>20.0:24.0, R2=>10.0:13.0]";
                List<Shape> result = sortShapes(shapes);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result.toString())) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test 2: Sort by Perimeter if Area is tied
        {
            try {
                ArrayList<Shape> shapes = new ArrayList<>();
                shapes.add(new Rectangle("R1", 8, 3)); // Area 24, Perim 22
                shapes.add(new Rectangle("R2", 6, 4)); // Area 24, Perim 20
                shapes.add(new Rectangle("R3", 12, 2)); // Area 24, Perim 28
                System.out.printf("Test %d: sortShapes tied areas%n", tcNum++);
                String expected = "[R3=>24.0:28.0, R1=>24.0:22.0, R2=>24.0:20.0]";
                List<Shape> result = sortShapes(shapes);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                if (expected.equals(result.toString())) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test 3: Mix of Circle and Rectangles
        {
            try {
                ArrayList<Shape> shapes = new ArrayList<>();
                shapes.add(new Rectangle("R1", 8, 3)); // Area 24, Perim 22
                shapes.add(new Circle("C1", 10)); // Area ~314, Perim ~63
                shapes.add(new Circle("C2", 1)); // Area ~3, Perim ~6
                System.out.printf("Test %d: sortShapes Circle/Rect mix%n", tcNum++);
                String expected = "[C1=>314.0:63.0, R1=>24.0:22.0, C2=>3.0:6.0]";
                List<Shape> result = sortShapes(shapes);
                System.out.printf("Expected  :|%s|%n", expected);
                System.out.printf("Actual    :|%s|%n", result);
                // Note: The toString format for Circles/Rects is used in student code
                if (expected.equals(result.toString())) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }

        // Test 4: Single element
        {
            try {
                ArrayList<Shape> shapes = new ArrayList<>();
                shapes.add(new Rectangle("R1", 1, 1));
                System.out.printf("Test %d: sortShapes single item%n", tcNum++);
                String expected = "[R1=>1.0:4.0]";
                List<Shape> result = sortShapes(shapes);
                if (expected.equals(result.toString())) {
                    score += 1;
                    System.out.println("Passed");
                } else {
                    System.out.println("Failed");
                }
            } catch (Exception e) {
                System.out.println("Failed -> Exception");
            }
            System.out.println("-------------------------------------------------------");
        }
    }
}
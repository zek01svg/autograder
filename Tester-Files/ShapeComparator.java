import java.util.*;   

public class ShapeComparator implements Comparator<Shape>{

    @Override
    public int compare(Shape s1, Shape s2) {
        // Sort by area in descending order
        if (s1.getArea() > s2.getArea()) {
            return -1;
        } else if (s1.getArea() < s2.getArea()) {
            return 1;
        } else {
            // If areas are equal, sort by perimeter in descending order
            if (s1.getPerimeter() > s2.getPerimeter()) {
                return -1;
            } else if (s1.getPerimeter() < s2.getPerimeter()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}

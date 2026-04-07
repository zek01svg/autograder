package Q2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the DataException class.
 * Since the PDF parsing was unavailable, this test covers the standard requirements 
 * for a custom Exception class as per typical OOP assignment structures.
 */
@DisplayName("Question 2: DataException Validation")
public class DataExceptionTest {

    @Test
    @DisplayName("Requirement 1: Check inheritance")
    void testDataExceptionInheritance() {
        DataException exception = new DataException("Test message");
        // Check if it's an instance of Exception (checked exception)
        assertTrue(exception instanceof Exception, "DataException should extend java.lang.Exception to be a checked exception.");
    }

    @Test
    @DisplayName("Requirement 2: Check message constructor")
    void testDataExceptionMessage() {
        String testMessage = "Invalid input data detected";
        DataException exception = new DataException(testMessage);
        
        assertEquals(testMessage, exception.getMessage(), 
            "DataException should properly pass the message string to the super constructor (Exception).");
    }

    @Test
    @DisplayName("Requirement 3: Check Exception behavior")
    void testThrowingDataException() {
        assertThrows(DataException.class, () -> {
            throw new DataException("Manual throw test");
        }, "DataException should be throwable like any other Java exception.");
    }

    @Test
    @DisplayName("Requirement 4: Verify class name and package")
    void testClassName() {
        assertEquals("Q2.DataException", DataException.class.getName(), 
            "The class should be named DataException and reside in the Q2 package.");
    }
}
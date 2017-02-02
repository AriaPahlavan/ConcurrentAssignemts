import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by Aria Pahlavan on 1/31/17.
 */
class PSearchTest {
    @BeforeEach
    void setUp() {
        
    }
    
    @Test
    void testParallelSearch(){
        int[] A = {5,8,12,6848,325,-12,456};
    
        System.out.println(PSearch.parallelSearch(-12, A, 1515646846));
    }
}
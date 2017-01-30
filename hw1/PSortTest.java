import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by Aria Pahlavan on 1/28/17.
 */
class PSortTest {
    int[] arr;
    
    @BeforeEach
    void setUp() {
        arr = new int[]{ 6, 2, 17, 5, 24, 0, 4, 0, 0, 45, 8, 1, 3, 9 , 3000, 50, 10, 95, 56, 7};
    }
    
    @Test
    void parallelSort() throws InterruptedException, ExecutionException {
    
        PSort.parallelSort(arr, 0, arr.length);
        
        Integer[] result = Arrays.stream(arr).boxed().toArray(Integer[]::new);
        
        System.out.println(Arrays.stream(result)
                                   .map(Object::toString)
                                   .collect(Collectors.joining(", ", "{", "}")));
    
    }
    
}
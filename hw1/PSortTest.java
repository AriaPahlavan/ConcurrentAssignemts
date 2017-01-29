import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Aria Pahlavan on 1/28/17.
 */
class PSortTest {
    int[] arr;
    
    @BeforeEach
    void setUp() {
        arr = new int[]{ 2, 17, 5, 24, 0, 0, 45, 1, 156 };
    }
    
    @Test
    void parallelSort() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newSingleThreadExecutor();
        
        PSort.Array = arr;
        PSort p = new PSort(0, arr.length);
        Future<?> submit = es.submit(p);
        
        while ( submit.get() != null ){}
        
        Arrays.stream(PSort.Array).forEach(System.out::println);

        es.shutdown();
    }
    
}
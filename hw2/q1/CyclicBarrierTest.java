import org.junit.jupiter.api.Test;

/**
 * Created by Aria Pahlavan on 2/9/17.
 */
class CyclicBarrierTest {
    @Test
    void await() throws InterruptedException {
        CyclicBarrier cb = new CyclicBarrier(3);
    
        cb.await();
        cb.await();
    }
}
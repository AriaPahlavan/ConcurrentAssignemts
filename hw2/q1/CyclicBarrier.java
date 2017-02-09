import java.util.concurrent.Semaphore;

/*
 * EID's of group members
 * 
 */
public class CyclicBarrier {
    private int parties;
    private int waitingThreads = 0;
    private Semaphore semaphore, lock;
    
    public CyclicBarrier(int parties) {
        this.parties = parties;
        semaphore = new Semaphore(0);
        lock = new Semaphore(1);
    }
    
    public int await() throws InterruptedException {
        // Waits until all parties have invoked await on this barrier.
        // If the current thread is not the last to arrive then it is
        // disabled for thread scheduling purposes and lies dormant until
        // the last thread arrives.
        // Returns: the arrival index of the current thread, where index
        // (parties - 1) indicates the first to arrive and zero indicates
        // the last to arrive.
       
        
        while ( semaphore.availablePermits() != 0 ) { }
    
        lock.acquire();
        int index = waitingThreads;
    
        waitingThreads++;
        
        if ( waitingThreads == parties ) {
            waitingThreads = 0;
            lock.release();
            semaphore.release(parties-1);

        } else {
            lock.release();
            semaphore.acquire();    //while(permits <= 0) {skip();}
        }
        
        // you need to write this code
        return index;
    }
}
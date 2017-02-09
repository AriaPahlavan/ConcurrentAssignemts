/*
 * EID's of group members
 * 
 */
public class MonitorCyclicBarrier {
    private int parties;
    private int waitingThreads = 0;
    
    public MonitorCyclicBarrier(int parties) {
        this.parties = parties;
    }
    
    public int await() throws InterruptedException {

        int index = awaitSynced();

        return index;
    }
    
    private synchronized int awaitSynced() throws InterruptedException {
        int index = waitingThreads++;
        
        if ( waitingThreads == parties ) {
            waitingThreads = 0;
            notifyAll();
        }
        
        else {
            wait();
        }
        
        return index;
    }
}
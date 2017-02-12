import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Garden {
    private final static  int DUG_HOLES_LIMIT = 4;
    private final static  int UNFILLED_HOLES_LIMIT = 8;
    private final ReentrantLock monitorLock = new ReentrantLock();
    private final Condition isEmptyHole = monitorLock.newCondition();
    private final Condition isSeedPlanted = monitorLock.newCondition();
    private final Condition isShovelTaken = monitorLock.newCondition();
    private final Condition reachedDugMax = monitorLock.newCondition();
    private final Condition reachedUnfilledMax = monitorLock.newCondition();
    
    private int holesDug = 0;
    private int seedsPlanted = 0;
    private int unfilledHoles = 0;
    
    public Garden() {
        
    }
    
    public void startDigging() throws InterruptedException {
        monitorLock.lock();
        try {
            System.out.println("Newton is digging hole " + holesDug + 1);
            isShovelTaken.await();
            
            if ( getUnseededHoles() == DUG_HOLES_LIMIT )
                reachedDugMax.await();
            
            if ( unfilledHoles == UNFILLED_HOLES_LIMIT )
                reachedUnfilledMax.await();
            
        } finally {
            monitorLock.unlock();
        }
    }
    
    public void doneDigging() {
        monitorLock.lock();
        try {
            holesDug++;
            System.out.println("Newton is done digging hole " + holesDug);

            unfilledHoles++;
            isEmptyHole.signal();   //TODO order matters?
            isShovelTaken.signal(); //TODO maybe signalAll() is sufficient
        } finally {
            monitorLock.unlock();
        }
    }
    
    public void startSeeding() throws InterruptedException{
        monitorLock.lock();
        try {
            System.out.println("Benjamin is seeding hole " + seedsPlanted+1);
            isEmptyHole.await();
            
        } finally {
            monitorLock.unlock();
        }
    }
    
    public void doneSeeding() {
        monitorLock.lock();
        try {
            seedsPlanted++;
            System.out.println("Benjamin is done seeding hole " + seedsPlanted);

            if(getUnseededHoles() < DUG_HOLES_LIMIT){
                reachedDugMax.signal();
            }
            isSeedPlanted.signal(); //TODO signalAll() ?
        } finally {
            monitorLock.unlock();
        }
    }
    
    public void startFilling() throws InterruptedException{
        monitorLock.lock();
        try {
            System.out.println("Mary is filling hole " + unfilledHoles);

            isShovelTaken.await();
            isSeedPlanted.await();
        
        } finally {
            monitorLock.unlock();
        }
    }
    
    public void doneFilling() {
        monitorLock.lock();
        try {
            unfilledHoles--;
            System.out.println("Mary is done filling hole " + unfilledHoles);
            if(unfilledHoles < UNFILLED_HOLES_LIMIT){
                reachedUnfilledMax.signal();
            }
            isShovelTaken.signal();

        
        } finally {
            monitorLock.unlock();
        }
    }
    
    /**
     * The following methods return the total number of holes dug, seeded or
     * filled by Newton, Benjamin or Mary at the time the methods' are
     * invoked on the garden class.
     */
    public int totalHolesDugByNewton() {

        return holesDug;
    }
    
    public int totalHolesSeededByBenjamin() {

        return seedsPlanted;
    }
    
    public int totalHolesFilledByMary() {

        return holesDug - unfilledHoles;
    }
    
    public int getUnseededHoles() {
        return holesDug - seedsPlanted;
    }
}

import java.security.Timestamp;
import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Garden {
    private final static int DUG_HOLES_LIMIT = 4;
    private final static int UNFILLED_HOLES_LIMIT = 8;
    private final ReentrantLock monitorLock = new ReentrantLock();
    private final Condition isEmptyHole = monitorLock.newCondition();
    private final Condition isSeedPlanted = monitorLock.newCondition();
    private final Condition isShovelTaken = monitorLock.newCondition();
    private final Condition reachedDugMax = monitorLock.newCondition();
    private final Condition reachedUnfilledMax = monitorLock.newCondition();

    private int holesDug = 0;
    private int seedsPlanted = 0;
    private int unfilledHoles = 0;
    private boolean shovelTaken = false;

    public Garden() {

    }

    public void startDigging() throws InterruptedException {
        monitorLock.lock();
        try {

            if (getUnseededHoles() == DUG_HOLES_LIMIT) {
                System.out.println("Newton is waiting for Benjamin to finish seeding");
                reachedDugMax.await();
            }

            if (unfilledHoles == UNFILLED_HOLES_LIMIT) {
                System.out.println("Newton is waiting for Mary to finish filling");
                reachedUnfilledMax.await();
            }


            while (shovelTaken) {
                System.out.println("Newton is waiting for the shovel");
                isShovelTaken.await();
            }
            System.out.println("Newton got the shovel");
            shovelTaken = true;
        } finally {
            System.out.println("Newton is digging hole " + (holesDug + 1) + " at " + (new Date().getTime() & 0xfff));
            monitorLock.unlock();
        }
    }

    public void doneDigging() {
        monitorLock.lock();
        try {
            newHoleDug();
            System.out.println("Newton is done digging hole " + holesDug);
            isEmptyHole.signal();   //TODO order matters?
            shovelTaken = false;
            isShovelTaken.signal(); //TODO maybe signalAll() is sufficient
        } finally {
            monitorLock.unlock();
        }
    }

    private void newHoleDug() {
        holesDug++;
        unfilledHoles++;
    }

    public void startSeeding() throws InterruptedException {
        monitorLock.lock();
        try {
            if(getUnseededHoles()<1) {
                System.out.println("Benjamin is waiting for an empty hole");
                isEmptyHole.await();
            }

        } finally {
            System.out.println("Benjamin is seeding hole " + (seedsPlanted + 1)+ " at " + (new Date().getTime() & 0xfff));
            monitorLock.unlock();
        }
    }

    public void doneSeeding() {
        monitorLock.lock();
        try {
            seedsPlanted++;
            System.out.println("Benjamin is done seeding hole " + seedsPlanted);

            if (getUnseededHoles() < DUG_HOLES_LIMIT) {
                reachedDugMax.signal();
            }
            isSeedPlanted.signal(); //TODO signalAll() ?
        } finally {
            monitorLock.unlock();
        }
    }

    public void startFilling() throws InterruptedException {
        monitorLock.lock();
        try {

            if(getUnfilledSeededHoles() < 1) {
                System.out.println("Mary is waiting for a seed to be planted");
                isSeedPlanted.await();
            }

            while (shovelTaken) {
                System.out.println("Mary is waiting for the shovel");
                isShovelTaken.await();
            }
            System.out.println("Mary got the shovel");
            shovelTaken = true;
        } finally {
            System.out.println("Mary is filling hole " + (totalHolesFilledByMary()+1)
                                + " at " + (new Date().getTime() & 0xfff));
            monitorLock.unlock();
        }
    }

    public void doneFilling() {
        monitorLock.lock();
        try {
            unfilledHoles--;
            System.out.println("Mary is done filling hole " + totalHolesFilledByMary());
            if (unfilledHoles < UNFILLED_HOLES_LIMIT) {
                reachedUnfilledMax.signal();
            }
            shovelTaken = false;
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

    public int getUnfilledSeededHoles() {
        return unfilledHoles - getUnseededHoles();
    }
}

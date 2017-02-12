import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;
import static org.junit.Assert.*;

/**
 * Created by Sharmistha on 2/12/2017.
 */
public class GardenTest {
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testGarden() throws Exception {
        final Garden garden = new Garden();
        final int plantsNeeded = 10;

        Thread newton = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < plantsNeeded; i++) {
                    try {
                        garden.startDigging();
                        sleep(1000);
                        garden.doneDigging();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

        Thread benjamin = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < plantsNeeded; i++) {
                    try {
                        garden.startSeeding();
                        sleep(1000);
                        garden.doneSeeding();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Thread mary = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < plantsNeeded; i++) {
                    try {
                        garden.startFilling();
                        sleep(1000);
                        garden.doneFilling();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        newton.start();
        benjamin.start();
        mary.start();

        newton.join();
        benjamin.join();
        mary.join();

        System.out.println("Total holes dug: "+ garden.totalHolesDugByNewton());
        System.out.println("Total holes seeded: "+ garden.totalHolesSeededByBenjamin());
        System.out.println("Total holes filled: "+ garden.totalHolesFilledByMary());
    }

}
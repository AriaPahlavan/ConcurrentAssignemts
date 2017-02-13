package lockTest;

import org.junit.Test;

import static java.lang.Thread.sleep;
import static org.junit.Assert.*;

/**
 * Created by Sharmistha on 2/12/2017.
 */
public class FairReadWriteLockTest {
    @Test
    public void fairReadTest() throws Exception {
        final FairReadWriteLock f = new FairReadWriteLock();
        Thread writerOne = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    f.beginWrite();
                    sleep(20);
                    f.endWrite();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread writerTwo = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    f.beginWrite();
                    sleep(5);
                    f.endWrite();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread readerOne = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    f.beginRead();
                    sleep(5);
                    f.endRead();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread readerTwo = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    f.beginRead();
                    sleep(5);
                    f.endRead();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        writerOne.start();
        readerOne.start();
        writerTwo.start();
        readerTwo.start();


        writerOne.join();
        readerOne.join();
        writerTwo.join();
        readerTwo.join();


    }

}
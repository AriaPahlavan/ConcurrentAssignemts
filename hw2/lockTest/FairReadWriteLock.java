package lockTest;
public class FairReadWriteLock {

    private int numReaders = 0;
    private boolean writerWriting = false;

    public synchronized void beginRead() {
        while(writerWriting){
            try {
                System.out.println("Reader "+(numReaders+1)+" is waiting");
                wait();
            } catch (InterruptedException e) {

            }
        }
        System.out.println("Reader "+(numReaders+1)+" begins to read");

        numReaders++;
    }

    public synchronized void endRead() {
        numReaders--;
        System.out.println("Reader "+(numReaders+1)+" is done reading");

        if(numReaders == 0){
            notify(); //TODO: notify or notify all?
        }
    }

    public synchronized void beginWrite() {
        while(numReaders > 0 || writerWriting){
            try {
                System.out.println("Writer is waiting");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Writer starts to write");
        writerWriting = true;
    }
    public synchronized void endWrite() {
        System.out.println("Writer is done writng");

        writerWriting = false;
        notifyAll();
    }
}



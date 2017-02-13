public class FairReadWriteLock {

	private int numReaders = 0;
	private boolean writerWriting = false;
                        
	public synchronized void beginRead() {
		while(writerWriting){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        numReaders++;
	}
	
	public synchronized void endRead() {
	    numReaders--;
	    if(numReaders == 0){
	        notify(); //TODO: notify or notify all?
        }
	}
	
	public synchronized void beginWrite() {
	    while(numReaders > 0 || writerWriting){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        writerWriting = true;
	}
	public synchronized void endWrite() {
	    writerWriting = false;
	    notifyAll();
	}
}
	

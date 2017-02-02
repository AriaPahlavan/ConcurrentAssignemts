//Name: Sharmistha Maity, Aria Pahlavan
//EID: SM47767, AP44342
//Course: EE 360P (16530)
//Assignment: Homework 1

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PSearch implements Callable<Integer> {
    private static ExecutorService threadpool = Executors.newCachedThreadPool();
    private int[] array;
    private int x;
    public static int q;
    
    public PSearch(int x, int[] A) {
        this.array = A;
        this.x = x;
    }
    
    
    public static int parallelSearch(int x, int[] A, int numThreads) {
        if ( numThreads <= 0)
            throw new IllegalArgumentException("'numThreads' must be positive");
        
        List<Future<Integer>> results = new ArrayList<>();
        q = A.length / numThreads;
        
        if ( numThreads >= A.length ) {
            q=1;
            for ( int i = 0; i < A.length; i++ ) {
                int A2[] = new int[1];
                A2[0] = A[i];
                PSearch p = new PSearch(x, A2);
                Future<Integer> task = threadpool.submit(p);
                results.add(task);
            }
        } else {
            int count = 0;
            for ( int i = 0; i < numThreads - 1; i++ ) {
                int A2[] = new int[q];
                System.arraycopy(A, count, A2, 0, q);
                PSearch p = new PSearch(x, A2);
                Future<Integer> task = threadpool.submit(p);
                results.add(task);
                count = count + q;
            }
            int remaining = (A.length / numThreads) + A.length % numThreads;
            int A2[] = new int[remaining];
            System.arraycopy(A, count, A2, 0, remaining);
            PSearch p = new PSearch(x, A2);
            Future<Integer> task = threadpool.submit(p);
            results.add(task);
        }
    
        for ( int i = 0; i < results.size(); i++ ) {
            try {
                int retVal = results.get(i).get();
                
                if ( retVal != -1 ) {
                    return (i * q) + retVal;
                }
                
            } catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
        }
        // if not found
        return -1;
    }
    
    private int sequentSearch() {
        for ( int i = 0; i < this.array.length; i++ ) {
            if ( this.array[i] == this.x ) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public Integer call() throws Exception {
        return sequentSearch();
    }
}
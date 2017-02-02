//Name: Sharmistha Maity, Aria Pahlavan
//EID: SM47767, AP44342
//Course: EE 360P (16530)
//Assignment: Homework 1

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PSort implements Runnable {
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    private int[] array;
    private int begin, end;
    
    public PSort(int[] array, int begin, int end) {
        this.array = array;
        this.begin = begin;
        this.end = end;
    }
    
    public static void parallelSort(int[] A, int begin, int end) {
        ExecutorService es = Executors.newSingleThreadExecutor();
        
        PSort p = new PSort(A, 0, A.length);
        Future<?> task = es.submit(p);
        
        try {
            task.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        
        es.shutdown();
    }
    
    private void parallelSortRec() {
        if ( begin == end )
            return;
        
        int sizeSubArr = end - begin;
        if ( sizeSubArr <= 4 ) {
            
            for ( int i = begin; i < end; i++ )
                for ( int j = end - 1; j > i; j-- )
                    if ( array[j] < array[i] )
                        swap(j, i);
        } else {
            
            int pivot = findPivot();
            
            
            PSort p1 = new PSort(array, begin, pivot);
            Future<?> sub_task1 = threadPool.submit(p1);
            
            PSort p2 = new PSort(array, pivot + 1, end);
            Future<?> sub_task2 = threadPool.submit(p2);
            
            try {
                sub_task1.get();
                sub_task2.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }
        
    }
    
    private int findPivot() {
        int hi = end - 1, lo = begin, i = lo;
        int pivot = array[hi];
        
        for ( int j = lo; j < hi; j++ ) {
            if ( array[j] <= pivot ) {
                swap(i, j);
                i++;
            }
        }
        
        swap(i, hi);
        return i;
    }
    
    private void swap(int j, int i) {
        int temp = array[j];
        array[j] = array[i];
        array[i] = temp;
    }
    
    @Override
    public void run() {
        parallelSortRec();
    }
}

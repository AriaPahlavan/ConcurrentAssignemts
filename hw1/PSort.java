import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Aria Pahlavan on 1/28/17.
 */
public class PSort implements Runnable {
    static ExecutorService threadPool = Executors.newCachedThreadPool();
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
        
        /*
        int pivot = array[begin];
        int[] newA = new int[array.length];
        int lo = begin+1, hi = end-1;
        
        for ( int i = begin+1; i < end; i++ ) {
            if ( array[i] > pivot) {
                newA[hi] = array[i];
                hi--;
            }
            else {
                newA[lo] = array[i];
                lo++;
            }
        }
        
        assert (hi == lo);
        newA[hi] = pivot;
        
        PivotObj obj = new PivotObj(newA, hi);
        return obj;
        */
    }
    
    private void swap(int j, int i) {
        int temp = array[j];
        array[j] = array[i];
        array[i] = temp;
    }
    
    private static int getMed(int begin, int end) {
        return (end - begin) / 2;
    }
    
    @Override
    public void run() {
        parallelSortRec();
    }
}

class PivotObj {
    private int[] a;
    private int pivot;
    
    public PivotObj(int[] a, int pivot) {
        this.a = a;
        this.pivot = pivot;
    }
    
    public int[] getA() {
        return a;
    }
    
    public int getPivot() {
        return pivot;
    }
    
    public void setPivot(int pivot) {
        this.pivot = pivot;
    }
    
    public void setA(int[] a) {
        this.a = a;
    }
}


/*    static ExecutorService threadPool = Executors.newCachedThreadPool();
    
    static int[] array;
    int begin, end;
    
    PSort(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }
    
    public static void parallelSortRec(int begin, int end){
        if ( begin == end )
            return;
        
        int sizeSubArr = end - begin;
        if ( sizeSubArr <= 4){
            
            for(int i=begin; i< end; i++)
                for ( int j=end-1; j>i; j--)
                    if ( array[j] < array[i] )
                        swap(array, j, i );
        }
        
        else {
            
            PivotObj pivot = findPivot(begin, end);
            
            array = pivot.getA();
            
            
            PSort p1 = new PSort(begin, pivot.getPivot());
            Future<?> sub_task1 = threadPool.submit(p1);
    
            PSort p2 = new PSort(pivot.getPivot()+1, end);
            Future<?> sub_task2 = threadPool.submit(p2);
    
            try {
                while ( sub_task1.get() != null || sub_task2.get() != null ){}
            }
            catch (InterruptedException | ExecutionException ignored) {}
        }
    
    }
    
    private static PivotObj findPivot(int begin, int end) {
        int pivot = array[begin];
        int[] newA = new int[array.length];
        int lo = begin+1, hi = end-1;
        
        for ( int i = begin+1; i < end; i++ ) {
            if ( array[i] > pivot) {
                newA[hi] = array[i];
                hi--;
            }
            else {
                newA[lo] = array[i];
                lo++;
            }
        }
        
        assert (hi == lo);
        newA[hi] = pivot;
        
        PivotObj obj = new PivotObj(newA, hi);
        return obj;
    }
    
    private static void swap(int[] A, int j, int i) {
        int temp = A[j];
        A[j] = A[i];
        A[i] = temp;
    }
    
    private static int getMed(int begin, int end) {
        return (end-begin)/2;
    }
    
    @Override
    public void run() {
        PSort.parallelSortRec(begin, end);
    }
    
    
}

class PivotObj{
    private int[] a;
    private int pivot;
    
    public PivotObj(int[] a, int pivot) {
        this.a = a;
        this.pivot = pivot;
    }
    
    public int[] getA() {
        return a;
    }
    
    public int getPivot() {
        return pivot;
    }
    
    public void setPivot(int pivot) {
        this.pivot = pivot;
    }
    
    public void setA(int[] a) {
        this.a = a;
    }
}*/
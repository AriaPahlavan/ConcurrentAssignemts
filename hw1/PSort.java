import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Aria Pahlavan on 1/28/17.
 */
public class PSort implements Runnable{
    static ExecutorService threadPool = Executors.newCachedThreadPool();
    
    static int[] Array;
    int begin, end;
    
    PSort(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }
    
    public static void parallelSort(int begin, int end){
        if ( begin == end )
            return;
        
        int sizeSubArr = end - begin;
        if ( sizeSubArr <= 4){
            
            for(int i=begin; i< end; i++)
                for ( int j=end-1; j>i; j--)
                    if ( Array[j] < Array[i] )
                        swap(Array, j, i );
        }
        
        else {
            
            PivotObj pivot = findPivot(begin, end);
            
            Array = pivot.getA();
            
            
            PSort p1 = new PSort(begin, pivot.getPivot());
            Future<?> submit = threadPool.submit(p1);
    
            PSort p2 = new PSort(pivot.getPivot()+1, end);
            Future<?> submit1 = threadPool.submit(p2);
    
            try {
                while ( submit.get() != null || submit1.get() != null ){}
            }
            catch (InterruptedException | ExecutionException ignored) {}
        }
    
    }
    
    private static PivotObj findPivot(int begin, int end) {
        int pivot = Array[begin];
        int[] newA = new int[Array.length];
        int lo = begin+1, hi = end-1;
        
        for ( int i = begin+1; i < end; i++ ) {
            if ( Array[i] > pivot) {
                newA[hi] = Array[i];
                hi--;
            }
            else {
                newA[lo] = Array[i];
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
        PSort.parallelSort(begin, end);
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
}
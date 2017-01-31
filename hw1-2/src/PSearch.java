//UT-EID=


import java.util.*;
import java.util.concurrent.*;


public class PSearch implements Callable {
	private static ExecutorService threadpool = Executors.newCachedThreadPool();
	private int[] array;
	private int x;
	public int q;

	public PSearch (int k, int[]A){
		this.array = A;
		this.x = k;
	}
	
	
	
	public static int parallelSearch(int k, int[] A, int numThreads){
	    // TODO: Implement your parallel search function 
		
		ArrayList<int[]> numArray = new ArrayList<int[]>();
		
		if(numThreads >= A.length){
			for(int i=0; i<A.length; i++){
			int A2[] = new int[1];
			System.arraycopy(A, i, A2, 0, 1);
			numArray.add(A2);
			}
		}
		else{
			int q = A.length/numThreads;
			int count = 0;
			for(int i=0; i<numThreads-1; i++){
				int A2[] = new int[q];
				System.arraycopy(A, count, A2, 0, q);
				numArray.add(A2);
				count = count+q;
			}
			int remaining = (A.length/numThreads) + A.length%numThreads;
			int A2[] = new int[remaining];
			System.arraycopy(A, count, A2, 0, remaining);
			numArray.add(A2);
		}
		
		
		
		// if not found
	}
	
	private int sequentSearch(){
		for(int i=0; i<this.array.length; i++){
			if(this.array[i]==this.x){
				return i;
			}
		}
		return -1;
	}
  
	@Override
	public Integer call() throws Exception {
		// TODO Auto-generated method stub
	
	}
}
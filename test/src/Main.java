import static java.lang.Thread.sleep;

/**
 * Created by aria on 2/25/17.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        int val = 10000;
        int[][] arr = new int[val][val];
        int sum = 0;
        long startTime;
        long first = 0, second = 0;

        for (int i = 0; i < val; i++)
            for (int j = 0; j < val; j++)
                arr[i][j] = (i + j) % val;

        sleep(1000);


        startTime = System.nanoTime();
        for (int i = 0; i < val; i++)
            for (int j = 0; j < val; j++)
                sum += arr[i][j];
        first = System.nanoTime() - startTime;

        sum = 0;
        sleep(1000);

        startTime = System.nanoTime();
        for (int i = 0; i < val; i++)
            for (int j = 0; j < val; j++)
                sum += arr[j][i];
        second = System.nanoTime() - startTime;



        System.out.println("Firs: " + (int)(first / 1e6) + "\nSecond: " + (int)(second/ 1e6));

    }
}

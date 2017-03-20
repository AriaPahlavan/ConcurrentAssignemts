import input.Order;
import input.User;

import java.io.*;
import java.net.*;
import java.util.*;
<<<<<<< Updated upstream
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
=======
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
>>>>>>> Stashed changes

public class Server {

    static Map<String, Integer> Inventory = new HashMap<>();
    static List<User> users = new ArrayList<>();
    static List<Order> allOrders = new ArrayList<>();
    static int orderID = 1;
    static final String DEFAULT_MODE = "U";
    static String curMode = DEFAULT_MODE;

    public static void main (String[] args) throws Exception {

        ExecutorService pool = Executors.newCachedThreadPool();
        int tcpPort;
        int udpPort;

        DatagramPacket datapacket, returnpacket;


        if (args.length != 3) {
            System.out.println("ERROR: Provide 3 arguments");
            System.out.println("\t(1) <tcpPort>: the port number for TCP connection");
            System.out.println("\t(2) <udpPort>: the port number for UDP connection");
            System.out.println("\t(3) <file>: the file of inventory");

            System.exit(-1);
        }
        tcpPort = Integer.parseInt(args[0]);
        udpPort = Integer.parseInt(args[1]);
        String fileName = args[2];

        // parse the inventory file
        File f = new File(fileName);
        Scanner s;

        try{
            s = new Scanner(f);
            while(s.hasNext()){
                Inventory.put(s.next(),s.nextInt());
            }
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }



        // TODO: handle request from clients
        DatagramSocket uSocket = new DatagramSocket(udpPort);
        ServerSocket tSocket = new ServerSocket(tcpPort);
        byte[] buf = new byte[1000];
        while(true){
            String request = "";
            String response = "";
            System.out.println(curMode);
            uSocket.setSoTimeout(1000);
            tSocket.setSoTimeout(1000);
            try {
                switch (curMode) {
                    case "U":
                        System.out.println("processing UDP");
                        datapacket = new DatagramPacket(buf, buf.length);
                        // uSocket.setSoTimeout(2000);
                        uSocket.receive(datapacket);
                        request = new String(datapacket.getData(), 0, datapacket.getLength());
                        //create new thread command
                        Callable<String> cmdUDP = new Command(request);
                        Future<String> respUDP = pool.submit(cmdUDP);
                        response = respUDP.get();
                        DatagramPacket responsePacket = new DatagramPacket(
                                response.getBytes(),
                                response.length(),
                                datapacket.getAddress(),
                                datapacket.getPort());

                        uSocket.send(responsePacket);
                        break;
                    case "T":
                        System.out.println("processing TCP");
                        curMode = "U";
                        try {
                            Socket clientSocket = tSocket.accept();
                            clientSocket.setSoTimeout(1000);
                            try {
                                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                                InputStream input = clientSocket.getInputStream();
                                BufferedReader in = new BufferedReader(new InputStreamReader((input)));
                                request = in.readLine();
                                //create thread here?
                                Callable<String> cmdTCP = new Command(request);
                                Future<String> respTCP = pool.submit(cmdTCP);
                                response = respTCP.get();
                                String[] responseSplit = response.split("\n");
                                for (int i = 0; i < responseSplit.length; i++) {
                                    out.println(responseSplit[i]);
                                }
                                out.flush();
                            }

                            finally {
                                clientSocket.close();
                            }

                        } finally {
                            //tSocket.close();
                        }

                        break;
                }
            }
            catch(SocketTimeoutException e){
<<<<<<< Updated upstream
                System.out.println("timeout caught");
                if(curMode == "U"){
=======
                if(curMode.equals("U")){
>>>>>>> Stashed changes
                    curMode = "T";
                }
                else {
                    curMode = "U";
                }
            }


        }

    }

}

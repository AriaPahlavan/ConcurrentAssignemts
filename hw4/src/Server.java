/**
 * Created by Sharmistha on 3/16/2017.
 */
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Server {

    static Map<Integer,ServerInfo> serverPorts = new HashMap<>();
    static List<ServerInfo> waitingServers = new ArrayList<>();
    static Map<String, Integer> Inventory = new HashMap<>();
    static List<User> users = new ArrayList<>();
    static List<Order> allOrders = new ArrayList<>();
    static int orderID = 1;

    public static void main (String[] args) throws IOException, ExecutionException, InterruptedException {

        ExecutorService pool = Executors.newCachedThreadPool();

        int numAck = 0;
        int myPort = -1;

        Scanner sc = new Scanner(System.in);
        int myID = sc.nextInt();
        int numServer = sc.nextInt();
        String inventoryPath = sc.next();

        String currentCommand = "";

        System.out.println("[DEBUG] my id: " + myID);
        System.out.println("[DEBUG] numServer: " + numServer);
        System.out.println("[DEBUG] inventory path: " + inventoryPath);
        for (int i = 0; i < numServer; i++) {
            // TODO: parse inputs to get the ips and ports of servers

            String str = sc.next();
            String[] ipPort = str.split(":");
            if(i == myID-1){myPort = Integer.parseInt(ipPort[1]);}
            ServerInfo newServer = new ServerInfo(i,Integer.parseInt(ipPort[1]));
            serverPorts.put(i+1,newServer);
            System.out.println("address for server " + i + ": " + str);
        }

        // parse the inventory file
        File f = new File(inventoryPath);
        Scanner scan = null;

        try{
            scan = new Scanner(f);
            while(scan.hasNext()){
                Inventory.put(scan.next(),scan.nextInt());
            }
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }

        while (true) {
            ServerSocket tSocket = new ServerSocket(myPort);
            String request = "";
            String response = "";
            try {
                Socket receivedSocket = tSocket.accept();
                try {
                    PrintWriter out = new PrintWriter(receivedSocket.getOutputStream(), true);
                    InputStream input = receivedSocket.getInputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader((input)));
                    request = in.readLine();
                    String[] reqSplit = request.split(" ");
                    if(reqSplit[0] == "request"){
                        int receivedPort = Integer.parseInt(reqSplit[1]);
                        int receivedID = Integer.parseInt(reqSplit[2]);
                        long receivedTimeStamp = ((long) Integer.parseInt(reqSplit[3]));

                        //insert received request into waitingServers
                        ServerInfo otherServer = serverPorts.get(receivedID);
                        otherServer.setTimeStamp(receivedTimeStamp);
                        serverPorts.put(myID,otherServer);
                        waitingServers.add(otherServer);

                        //send acknowledgement
                        String s = "acknowledgement";
                        Socket otherSocket = new Socket("localhost",receivedPort);
                        PrintWriter otherOut = new PrintWriter(otherSocket.getOutputStream());
                        otherOut.println(s);
                        otherOut.flush();

                    }
                    else if(reqSplit[0] == "acknowledgement"){
                        numAck = numAck+1;

                        //TODO: if received all acknowledgements and timestamp is smallest -> enterCS
                        if(numAck == numServer - 1) { //TODO: need to add check for smallest timeStamp
                            //enter CS and process command
                            Callable<String> cmdTCP = new Command(currentCommand);
                            Future<String> respTCP = pool.submit(cmdTCP);
                            response = respTCP.get();
                            String[] responseSplit = response.split("\n");
                            Socket clientSocket = new Socket("localhost",serverPorts.get(myID).getPort());
                            PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream());
                            for (int i = 0; i < responseSplit.length; i++) {
                                clientOut.println(responseSplit[i]);
                            }
                            clientOut.flush();

                            //release from CS
                                //TODO: remove self from waiting servers
                                //send release to all other servers
                            //send request to all other servers
                            String s = "release "+myID;
                            for(int i=0; i<serverPorts.size(); i++){
                                Socket otherSocket = new Socket("localhost",serverPorts.get(i+1).getPort());
                                PrintWriter otherOut = new PrintWriter(otherSocket.getOutputStream());
                                otherOut.println(s);
                                otherOut.flush();
                            }
                        }

                    }
                    else if(reqSplit[0] == "release"){
                        int receivedID = Integer.parseInt(reqSplit[1]);
                        //TODO: delete the request from waiting servers

                        //TODO: if received all acknowledgements and timestamp is smallest -> enterCS and then release


                    }
                    else if(reqSplit[0] == "command"){
                        //send request to all other servers
                        long myTimeStamp = System.currentTimeMillis() % 1000;
                        String s = "request " + myPort + " " + myID + " " + myTimeStamp;
                        for(int i=0; i<serverPorts.size(); i++){
                            Socket otherSocket = new Socket("localhost",serverPorts.get(i+1).getPort());
                            PrintWriter otherOut = new PrintWriter(otherSocket.getOutputStream());
                            otherOut.println(s);
                            otherOut.flush();
                        }

                        //insert self into waiting servers
                        ServerInfo myServer = serverPorts.get(myID);
                        myServer.setTimeStamp(myTimeStamp);
                        serverPorts.put(myID,myServer);
                        waitingServers.add(myServer);

                        //set number of acknowledgements to 0
                        numAck = 0;

                        //save command from client
                        currentCommand = "";
                        for(int i=1; i<reqSplit.length; i++){
                            currentCommand = currentCommand + reqSplit[i] + " ";
                        }

                    }
                    else{
                        System.out.println("error receiving requests");
                    }
                }

                finally {
                    receivedSocket.close();
                }

            } finally {
                //tSocket.close();
                break;
            }

        }
        // TODO: start server socket to communicate with clients and other servers

        // TODO: parse the inventory file

        // TODO: handle request from client
    }
}

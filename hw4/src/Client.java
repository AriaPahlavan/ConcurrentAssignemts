/**
 * Created by Sharmistha on 3/16/2017.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class Client {

    static List<Integer> serverPorts = new ArrayList<>();
    public static void main (String[] args) throws IOException {
        String hostAddress = "";


        Scanner sc = new Scanner(System.in);
        int numServer = sc.nextInt();

        for (int i = 0; i < numServer; i++) {
            // TODO: parse inputs to get the ips and ports of servers
            String line = sc.next();
            String[] ipPort = line.split(":");
            hostAddress = ipPort[0];
            serverPorts.add(Integer.parseInt(ipPort[1]));
        }

        //InetAddress ia = InetAddress.getByName(hostAddress);


        while(sc.hasNextLine()) {
            String cmd = sc.nextLine();
            String[] tokens = cmd.split(" ");

            if (tokens[0].equals("purchase")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
                process(cmd,numServer);
            } else if (tokens[0].equals("cancel")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
                process(cmd,numServer);
            } else if (tokens[0].equals("search")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
                process(cmd,numServer);
            } else if (tokens[0].equals("list")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
                process(cmd, numServer);
            } else {
                System.out.println("ERROR: No such command");
            }
        }
    }

    private static void process(String cmd, int serverCount) throws IOException {
        StringBuilder input = new StringBuilder();
        int tcpPort;
        for(int i=0; i<serverCount; i++){
            tcpPort = serverPorts.get(i);
            Socket tSocket = new Socket("localhost",tcpPort);
            PrintWriter out = new PrintWriter(tSocket.getOutputStream());
            //TODO: if connection is not established (100 milliseconds), loop to next tcpPort, otherwise complete statements below

            out.println(cmd);
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(tSocket.getInputStream()));
            String line = "";
            while((line = in.readLine()) != null) {
                System.out.println(line);
                input.append(line);
                input.append('\n');

            }
            break; //if this point is reached then break for loop cause connection was established
        }

    }
}

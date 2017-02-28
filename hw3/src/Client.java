import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main (String[] args) throws IOException {
        String hostAddress;
        int tcpPort;
        int udpPort;
        final String DEFAULT_MODE = "U";
        String curMode = DEFAULT_MODE;

        DatagramPacket spacket;
        byte[] buffer;
        int length;



        if (args.length != 3) {
            System.out.println("ERROR: Provide 3 arguments");
            System.out.println("\t(1) <hostAddress>: the address of the server");
            System.out.println("\t(2) <tcpPort>: the port number for TCP connection");
            System.out.println("\t(3) <udpPort>: the port number for UDP connection");
            System.exit(-1);
        }

        hostAddress = args[0];
        tcpPort = Integer.parseInt(args[1]);
        udpPort = Integer.parseInt(args[2]);
        InetAddress ia = InetAddress.getByName(hostAddress);

        System.out.println("Enter command: ");
        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()) {
            String cmd = sc.nextLine();
            String[] tokens = cmd.split(" ");
            buffer = cmd.getBytes();
            length = buffer.length;
            spacket = new DatagramPacket(buffer, length, ia, udpPort);


            if (tokens[0].equals("setmode")) {
                // TODO: set the mode of communication for sending commands to the server
                // and display the name of the protocol that will be used in futures
                if(!curMode.equals(tokens[1].toUpperCase())){
                    switch(curMode) {
                        case "U":
                            processUDP(spacket);
                            break;
                        case "T":
                            processTCP(cmd, tcpPort); //TODO: process TCP
                            break;
                    }
                }
                curMode = tokens[1];
                System.out.println(curMode);
                if(curMode.toUpperCase().equals("U")){
                    System.out.println("Protocol for Communication: UDP");
                }
                else{
                    System.out.println("Protocol for Communication: TCP");
                }
            }
            else if (tokens[0].equals("purchase")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server

                switch(curMode){
                    case "U":
                        processUDP(spacket);
                        break;
                    case "T":
                        processTCP(cmd,tcpPort);
                        break;

                }

            } else if (tokens[0].equals("cancel")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
                switch(curMode) {
                    case "U":
                        processUDP(spacket);
                        break;
                    case "T":
                        processTCP(cmd,tcpPort); //TODO: process TCP
                        break;
                }

            } else if (tokens[0].equals("search")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
                switch(curMode) {
                    case "U":
                        processUDP(spacket);
                        break;
                    case "T":
                        processTCP(cmd,tcpPort); //TODO: process TCP
                        break;
                }
            } else if (tokens[0].equals("list")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
                switch(curMode) {
                    case "U":
                        processUDP(spacket);
                        break;
                    case "T":
                        processTCP(cmd,tcpPort); //TODO: process TCP
                        break;
                }

            } else {
                System.out.println("ERROR: No such command");
            }

            System.out.println("");
            System.out.println("Enter command: ");
        }
    }

    static void processUDP(DatagramPacket sPacket) throws IOException {
        System.out.println("processing UDP");

        int rlength = 1000;
        byte[] rBuffer = new byte[rlength];
        DatagramPacket rPacket;
        DatagramSocket uSocket = new DatagramSocket();

        uSocket.send(sPacket);
        rPacket = new DatagramPacket(rBuffer, rlength);
        uSocket.receive(rPacket);
        System.out.println(new String(rPacket.getData(), 0, rPacket.getLength()));
    }

    static void processTCP(String s, int port) throws IOException {
        System.out.println("processing TCP");

        StringBuilder input = new StringBuilder();
        Socket tSocket = new Socket("localhost",port);
        PrintWriter out = new PrintWriter(tSocket.getOutputStream());
        out.println(s);
        out.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(tSocket.getInputStream()));
        String line = "";
        while ((line = in.readLine()) != null) {
            System.out.println(line);
            input.append(line);
            input.append('\n');
        }
        //tSocket.close();

    }
}

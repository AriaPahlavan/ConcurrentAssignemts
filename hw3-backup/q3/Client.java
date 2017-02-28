import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.Scanner;

public class Client {
    final static String UDP = "U", TCP = "T";

    public static void main(String[] args) throws IOException {
        String hostAddress;
        int tcpPort;
        int udpPort;
        final String DEFAULT_MODE = UDP;
        String curMode = DEFAULT_MODE;

        Socket tSocket;
        DatagramPacket sPacket, rPacket;

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
        StringBuilder sb = new StringBuilder();
        InetAddress ia = InetAddress.getByName(hostAddress);
        byte[] buffer;
        int length;

        System.out.print("Enter command: ");
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String cmd = sc.nextLine();
            String[] tokens = cmd.split(" ");

            buffer = cmd.getBytes();
            length = buffer.length;
            sPacket = new DatagramPacket(buffer, length, ia, udpPort);


            if (tokens[0].equals("setmode")) {
                String nextMode = tokens[1];
                if(!nextMode.equals(curMode)){
                    switch (curMode){
                        case UDP:
                            processUDP(sPacket);
                            break;
                        case TCP:
                            //TODO
                            break;
                    }
                }
                curMode = nextMode;
                System.out.println("Protocol for communication: " + (curMode.equals(UDP) ? "UDP" : "TCP"));
            } else if (tokens[0].equals("purchase")) {
                switch (curMode) {
                    case UDP:
                        processUDP(sPacket);
                        break;
                    case TCP:
                        break;
                }
            } else if (tokens[0].equals("cancel")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
                switch (curMode) {
                    case UDP:
                        processUDP(sPacket);
                        break;
                    case TCP:
                        break;
                }
            } else if (tokens[0].equals("search")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
                switch (curMode) {
                    case UDP:
                        processUDP(sPacket);
                        break;
                    case TCP:
                        break;
                }
            } else if (tokens[0].equals("list")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
            } else {
                System.out.println("ERROR: No such command");
            }

            System.out.print("Enter command: ");
        }
    }

    static void processUDP(DatagramPacket sPacket) throws IOException {
        int rlen = 1000;
        byte[] rBuffer = new byte[rlen];
        DatagramPacket rPacket;
        DatagramSocket uSocket = new DatagramSocket();
        uSocket.send(sPacket);

        rPacket = new DatagramPacket(rBuffer, rlen);
        uSocket.receive(rPacket);
        System.out.println(new String(rPacket.getData(), 0, rPacket.getLength()));
    }
}

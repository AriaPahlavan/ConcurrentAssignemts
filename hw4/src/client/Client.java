package client;//EIDs: sm47767, ap44342

import server.Server;
import util.ServerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static server.Server.CMD;

public class Client {

	private static List<ServerInfo> servers = new ArrayList<>();
	public final static int CONNECTION_TIMEOUT = 100;


	public static void main(String[] args) throws IOException {
		Scanner system_in  = new Scanner(System.in);
		int     numServers = system_in.nextInt();

		system_in.nextLine();

		for (int i = 0; i < numServers; i++) {
			String[]   ipAndPort = system_in.nextLine().split(":");
			ServerInfo pair      = new ServerInfo(ipAndPort[0], new Integer(ipAndPort[1]));

			servers.add(pair);
		}

		System.out.print("Enter command: ");

		while (system_in.hasNextLine()) {
			makeServerRequest(system_in.nextLine());
			System.out.print("Enter command: ");
		}
	}

	private static void makeServerRequest(String command) throws IOException {
		boolean        lookingForAliveServer = true;
		PrintWriter    socket_out;
		BufferedReader socket_in;

		while (lookingForAliveServer) {
			Optional<ServerInfo> maybeServer = servers.stream().filter(ServerInfo::isAvail).findFirst();

			if (maybeServer.isPresent())
				System.out.println("[DEBUG] Found server: " + maybeServer.get());
			else {
				System.out.println("[ERROR] No Servers Found.");
				System.exit(1);
			}

			ServerInfo aServer      = maybeServer.get();
			String     host         = aServer.getHost();
			Integer    port         = aServer.getPort();
			Socket     clientSocket = new Socket();
			String     respLine;


			try {
				clientSocket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
			}
			catch (SocketTimeoutException e) {
				killThisServer(aServer);
				continue;
			}
			catch (IOException e) {
				killThisServer(aServer);
				continue;
			}

			clientSocket.setSoTimeout(CONNECTION_TIMEOUT);

			socket_out = new PrintWriter(clientSocket.getOutputStream());
			socket_in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

//			System.out.println("[DEBUG] Sending command at " + Clock.systemUTC().millis());
			socket_out.println(CMD + " " + command);
			socket_out.flush();

			try {
//				System.out.println("[DEBUG] Waiting for ack at " + Clock.systemUTC().millis());
				while ((respLine = socket_in.readLine()) != null) {

					String[] respTok = respLine.split(" ");

					switch (respTok[0]) {
						case Server.ACK:
//						System.out.println("[DEBUG] Received ack at " + Clock.systemUTC().millis());
							break;
						case Server.END:
							return;
						default:
//						System.out.println("[DEBUG] Received response at " + Clock.systemUTC().millis());
							System.out.println(respLine);
							break;
					}
				}
			}
			catch (IOException e) {
				killThisServer(aServer);
			}
		}


	}

	private static void killThisServer(ServerInfo aServer) {
		servers.get(servers.indexOf(aServer)).killServer();
	}
}

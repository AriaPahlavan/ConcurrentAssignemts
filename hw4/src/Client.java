import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Client {

	static List<ServerInfo> servers = new ArrayList<>();
	final static int CONNECTION_TIMEOUT = 100;

	public static void main(String[] args) throws IOException {
		Scanner system_in 		= new Scanner(System.in);
		int numServers 			= system_in.nextInt();

		system_in.nextLine();

		for (int i = 0; i < numServers; i++) {
			String[] ipAndPort = system_in.nextLine().split(":");
			ServerInfo pair = new ServerInfo(ipAndPort[0], new Integer(ipAndPort[1]));

			servers.add(pair);
		}

		System.out.print("Enter command: ");

		while (system_in.hasNextLine()) {
			makeServerRequest(system_in.nextLine());
			System.out.print("Enter command: ");
		}
	}

	private static void makeServerRequest(String command) throws IOException {
		boolean lookingForAliveServer = true;
		PrintWriter socket_out;
		BufferedReader socket_in;

		while (lookingForAliveServer) {
			Optional<ServerInfo> anOptionalServer = servers.stream().filter(ServerInfo::isAvail).findFirst();

			if (anOptionalServer.isPresent())
				System.out.print("");
			else {
				System.out.println("[ERROR] No Servers Found.");
				System.exit(1);
			}

			ServerInfo aServer 			= anOptionalServer.get();
			String host 				= aServer.getHost();
			Integer port 				= aServer.getPort();
			Socket clientSocket			= new Socket();
			String respLine;


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

			socket_out.println(Server.CMD + " " + command);
			socket_out.flush();

			try {
				while ((respLine = socket_in.readLine()) != null) {

					String[] respTok = respLine.split(" ");

					if (respTok[0].equals(Server.ACK)) {
						continue;
					}
					else if (respTok[0].equals(Server.END)) return;

					System.out.println(respLine);
				}
			} catch (IOException e) {
				killThisServer(aServer);
				continue;
			}

		}


	}

	static void killThisServer(ServerInfo aServer) {
		servers.get(servers.indexOf(aServer)).killServer();
	}
}

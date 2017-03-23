import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

import static java.lang.System.exit;
import static java.lang.Thread.sleep;

public class Server {
/* Aria:
1 2 src/input/inventory.txt
localhost:8001
localhost:8002
*/

	final static String 						ACK 			= "[ACK]";
	final static String 						CMD 			= "[CMD]";
	final static String 						REQ 			= "[REQ]";
	final static String 						REL 			= "[REL]";
	final static String 						CMP 			= "[CMP]";
	final static String 						END 			= "[END]";

	private static Map<Integer,ServerInfo> 		servers 		= new HashMap<>();
	private static Map<Long, ServerInfo> 		waitingServers	= new HashMap<>();		//TODO what if a server wan't to process two command.
	private static Map<String, Integer> 		Inventory 		= new ConcurrentHashMap<>();
    private static List<User> 					users 			= new ArrayList<>();
	private static Map<Integer, OrderUserPair> 	allOrders 		= new ConcurrentHashMap<>();
	private static int 							orderID 		= 1;
	private static int 							numServers;

	public static void main (String[] args) throws Exception {
//		System.out.println("Enter <server-id> <n> <inventory-path>");
//		System.out.println("Enter <ip-address>:<port-number>");

		//
		//Scan inputs
		Scanner sc 				= new Scanner(System.in);
		int myID 				= sc.nextInt();
		int totServers 			= sc.nextInt();
		String inventoryPath 	= sc.next();
		File f 					= new File(inventoryPath);
		numServers 				= totServers;
		int myPort 				= extractServers(sc, myID);

		loadInventory(f);

		ServerSocket serverSocket = new ServerSocket(myPort);
		List<Socket> allSockets = new ArrayList<>();

		while(true){
			ServerInfo myInfo = servers.get(myID);

			Socket externalSocket = serverSocket.accept();
			allSockets.add(externalSocket);

			InputStream input = externalSocket.getInputStream();
			BufferedReader socket_in = new BufferedReader(new InputStreamReader((input)));
			String request = socket_in.readLine();
			String[] reqTok = request.split(" ");


			//Spawn a new thread to handle incoming messages!
			Runnable runnable = () -> handler(externalSocket, myInfo, request);
			new Thread(runnable).start();

			if (!reqTok[0].equals(CMD)) externalSocket.close();

		}

	}

	private final static BiConsumer<Socket, String[]> clientAckHandler = (socket, idAndReq) -> {
		PrintWriter client_out;

		Optional<PrintWriter> maybeClient = SocketStreams.getOutStream
				.apply(socket);

		if (maybeClient.isPresent()) client_out = maybeClient.get();
		else return;

		try {

			while (!Thread.currentThread().isInterrupted()) {
				//send ack to client
				client_out.println(ack(Integer.parseInt(idAndReq[0]), idAndReq[1]));
				sleep(80);
			}

		} catch (InterruptedException e) {
			System.out.println("[DEBUG] acknowledge thread ended.");
			return;
		}
	};

	private final static void handler(Socket externalSocket, ServerInfo myInfo, String request){
		System.out.println("[DEBUG] entered handler");
		int myID 			= myInfo.getID();
//		String request 		= "";
		String response;
		Integer receivedID;

		PrintWriter client_out = null;
		InputStream input;



		String[] reqTok = request.split(" ");

		//debug{
//		System.out.println("before striping off the tag:");
//		Arrays.stream(reqTok).forEach(s -> System.out.print(s +" "));
//		System.out.println();
		//}end

		request = request.substring(6);

		switch (reqTok[0]) {
			case CMD:
				System.out.println("[DEBUG] command received: " + request);


				try {

					client_out = new PrintWriter(externalSocket.getOutputStream(), true);
					input = externalSocket.getInputStream();
					BufferedReader socket_in = new BufferedReader(new InputStreamReader((input)));
//					request = socket_in.readLine();

				} catch (IOException e) {
					e.printStackTrace();
				}

				String[] idAndReq = new String[]{String.valueOf(myID), request};
				Runnable runnable = () -> clientAckHandler.accept(externalSocket, idAndReq);
				Thread clientAcknowledger = new Thread(runnable);
				clientAcknowledger.start();

				//send request to all other servers

				long myTimeStamp = System.currentTimeMillis() % 1000000;
				myInfo.setTimeStamp(myTimeStamp);

				String reqMsg = REQ + " " + myInfo.getPort()
									+ " " + myInfo.getHost()
									+ " " + myInfo.getID()
									+ " " + myInfo.getTimeStamp();

				broadcastToOtherServers(myID, reqMsg);

				//debug{
//				System.out.println("req msg: " + reqMsg);
				//}end

				//insert self into waiting servers
				myInfo.setTimeStamp(myTimeStamp);
				servers.put(myID, myInfo);
				addWaitingServer(myInfo);


				while (true) {
					//check for smallest timeStamp
					ServerInfo firstServerInline = getFirstServerInline();


					if (firstServerInline.equals(myInfo)) {
						//enter CS and process command
						response = processCommand(request);

						//debug{
						System.out.println("response to client: " + response);
						//}end

						clientAcknowledger.interrupt();

						try {
							clientAcknowledger.join();
						} catch (InterruptedException ignored) {}

						client_out.println(response);
						client_out.println(END);
						client_out.flush();

						//release from CS
						//send release to all other servers
						removeWaitingServer(myInfo.getTimeStamp());

						String relMsg = REL + " " + myInfo.getTimeStamp() + " " + request;		//TODO myID could correspond to multiple elements in waitingServers
						System.out.println("[DEBUG] Sending release to other servers: " + relMsg);
						notifyServers(myID, relMsg);
						break;
					}
					else {
						//check if server first in line is alive
						try {

							Socket firstServer = new Socket(firstServerInline.getHost(), firstServerInline.getPort());

						} catch (IOException e) {
							System.out.println("First server in line was dead: " + firstServerInline);
							removeAllWaiting(firstServerInline);
							killThisServer(firstServerInline.getID());
						}
					}
				}
				break;
			case REQ:
				//debug{
				System.out.println("[DEBUG] received request: ");
				Arrays.stream(reqTok).forEach(s -> System.out.print(s +" "));
				//}end


				Integer receivedPort 	= Integer.parseInt(reqTok[1]);
				String receivedHost 	= reqTok[2];
				receivedID 				= Integer.parseInt(reqTok[3]);
				Long receivedTimeStamp 	= Long.parseLong(reqTok[4]);

				//insert received request into waitingServers
				ServerInfo otherServer = servers.get(receivedID);
				otherServer.setTimeStamp(receivedTimeStamp);
				servers.put(receivedID, otherServer);
				addWaitingServer(otherServer);

				//send acknowledgement
				Socket otherSocket;

				try {

					otherSocket = new Socket(receivedHost, receivedPort);
					PrintWriter otherOut;
					otherOut = new PrintWriter(otherSocket.getOutputStream());

					otherOut.println(ACK + " server " + myInfo + " received request: " + request);
					otherOut.flush();

				} catch (IOException e) {
					e.printStackTrace();
				}

				break;
			case REL:
				System.out.println("[DEBUG] received release: "+ request);

				receivedTimeStamp = Long.parseLong(reqTok[1]);

				//delete the request from waiting servers
				System.out.println("REL -> my waitinglist: ");
				waitingServers.forEach((aLong, serverInfo) -> System.out.println("server: " + serverInfo + ", time stamp: " + aLong));

				removeWaitingServer(receivedTimeStamp);

				//update inventory or orders
				int i;

				for (i = 0; i < request.length(); i++) {
					if( request.toCharArray()[i] == ' ' )
						break;

				}

				String command = request.substring(i+1);

				processCommand(command);

				String compMsg = CMP + " " + receivedTimeStamp + " " + command;
				notifyServers(myID, compMsg);
				break;
			case CMP:
				System.out.println("[DEBUG] received completion: " + request);

				receivedTimeStamp = Long.parseLong(reqTok[1]);

				//check if the server is still in waitingList
				System.out.println("CMP -> my waitinglist: ");
				waitingServers.forEach((aLong, serverInfo) -> System.out.println("server: " + serverInfo + ", time stamp: " + aLong));

				if (removeWaitingServer(receivedTimeStamp) != null) {
					//update inventory or orders
					int j;

					for (j = 0; j < request.length(); j++) {
						if( request.toCharArray()[j] == ' ' )
							break;

					}

					String completedCommand = request.substring(j+1);
					System.out.println("[DEBUG] in completion, command: " + completedCommand);
					processCommand(completedCommand);
				}
				break;
			case ACK:
				System.out.println("ACK received in handler");
				break;
			default:
				System.out.println("[ERROR] Invalid message received.");
				break;
		}
	}

	private static synchronized ServerInfo getFirstServerInline() {
		Collection<ServerInfo> values = waitingServers.values();
		return values.stream()
				.filter(ServerInfo::isAvail)
				.min(Comparator.comparing(ServerInfo::getTimeStamp)).get();
	}

	private static synchronized void addWaitingServer(ServerInfo otherServer) {
		ServerInfo newServerTask = ServerInfo.dupServer(otherServer);

		waitingServers.put(otherServer.getTimeStamp(), newServerTask);
	}

	private static synchronized ServerInfo removeWaitingServer(Long timeStamp) {
		return waitingServers.remove(timeStamp);
	}

	private static synchronized void removeAllWaiting(ServerInfo s){
		waitingServers.forEach((timeStamp, server) -> {
			if (Objects.equals(server.getID(), s.getID()))
				waitingServers.remove(timeStamp);
		});
	}

	private static int extractServers(Scanner sc, int myID) {
		int myPort = 0;

		for (int id = 1; id <= numServers; id++) {

			String ipAndPort 		= sc.next();
			String[] tokens 		= ipAndPort.split(":");
			String host				= tokens[0];
			Integer port 			= Integer.parseInt(tokens[1]);
			ServerInfo newServer 	= new ServerInfo(id, host, port);

			if (id == myID) myPort = port;

			servers.put(id, newServer);
		}

		return myPort;
	}

	private static void loadInventory(File f) {
		Scanner file_sc;
		try{
			file_sc = new Scanner(f);

			while(file_sc.hasNext()){
				Inventory.put(file_sc.next(), file_sc.nextInt());
			}

		} catch(FileNotFoundException e){
			System.out.println("[ERROR] File Not Found.");
		}
	}

	private static void notifyServers(int myID, String msg) {

		for (int id = 1; id <= servers.size(); id++) {

			if (id == myID) continue;
			if (servers.get(id).isCrashed()) continue;

			ServerInfo otherServer = servers.get(id);
			Socket otherSocket;

			try {

				otherSocket = new Socket(otherServer.getHost(), otherServer.getPort());

				PrintWriter servers_out = SocketStreams.getOutStream.apply(otherSocket).get();

				servers_out.println(msg);
				servers_out.flush();

			} catch (IOException e) {
//				System.out.println("IO Exception while waiting for an input:");
//				killThisServer(otherServer.getID());
//				continue;
			}


			if (myID == 1 && id == 2) exit(1);

		}

	}

	private static void broadcastToOtherServers(int myID, String msg) {
		System.out.println("[DEBUG] entered broadcaster");

		for (int id = 1; id <= servers.size(); id++) {

			if (id == myID) continue;
			if (servers.get(id).isCrashed()) continue;

			ServerInfo otherServer = servers.get(id);
			Socket otherSocket;

			try {

				otherSocket = new Socket(otherServer.getHost(), otherServer.getPort());
				otherSocket.setSoTimeout(Client.CONNECTION_TIMEOUT);

				PrintWriter servers_out = SocketStreams.getOutStream.apply(otherSocket).get();
				BufferedReader servers_in = SocketStreams.getInStream.apply(otherSocket).get();

				System.out.println("requesting CS from: " + otherServer + ", with msg: ");
				System.out.println(msg);

				servers_out.println(msg);
				servers_out.flush();


				String respLine;

				while ((respLine = servers_in.readLine()) != null) {
					System.out.println(respLine);

					String[] respTok = respLine.split(" ");
					if (respTok[0].equals(ACK)) break;
				}

			} catch (IOException e) {
				System.out.println("Did not get response from server: " + otherServer);
				killThisServer(otherServer.getID());
				continue;
			}

		}

	}

	private static String ack(int myID, String request) {
		return (ACK + " " +servers.get(myID).toString() + " received the request: " + request);
	}

	private static String processCommand(String req) {
		String[] tokens = req.split(" ");
		String result = "";
		switch (tokens[0]) {
			case "purchase":
				String name = tokens[1];
				String product = tokens[2];
				Integer quantity = new Integer(tokens[3]);

				result = tryPurchase(name, product, quantity);
				break;
			case "cancel":
				Integer id = new Integer(tokens[1]);
				result = cancelOrder(id);
				break;

			case "search":
				String UserName = tokens[1];
				result = findUserOrders(UserName);
				break;

			case "list":
				result = listAllProducts();
				break;
		}
		return result;
	}

	static void killThisServer(int serverId) {
		System.out.println("Server " + servers.get(serverId).toString() + " is crashed." );
		servers.get(serverId).killServer();
	}

	private static String listAllProducts() {
		String result = "";
		Iterator it = getInventoryIterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			result = result + pair.getKey() + " " + pair.getValue() + "\n";
		}

		return result;
	}

	private static synchronized Iterator<Map.Entry<String, Integer>> getInventoryIterator() {
		return Inventory.entrySet().iterator();
	}

	private static synchronized String findUserOrders(String userName) {
		String result = "";
		User user = new User(userName);
		User foundUser;
		int userIndex;
		if (users.contains(user)) {
			userIndex = users.indexOf(user);
			foundUser = users.get(userIndex);
			for (int i = 0; i < foundUser.orders.size(); i++) {
				Order order = foundUser.orders.get(i);
				Integer orderID = order.getOrderID();
				String productName = order.getProduct();
				Integer quantity = order.getQuantity();
				result = result + orderID + " " + productName + " " + quantity + "\n";
			}
		} else {
			result = "No order found for " + userName;
		}
		return result;

	}

	private static synchronized String cancelOrder(Integer id) {
		String result;
		OrderUserPair orderUserPair;

		if ( (orderUserPair = allOrders.get(id)) != null) {
			Order order = orderUserPair.getOrder();

			String product = order.getProduct();
			int quantity = order.getQuantity();

			User user = orderUserPair.getUser();
			if (user.removeOrder(id))
				users.remove(user);

			Inventory.put(product, Inventory.get(product) + quantity);
			allOrders.remove(id);

			result = "Order " + id + " is canceled";
		}
		else result = id + " not found, no such order";

		return result;
	}

	private static synchronized String tryPurchase(String name, String product, Integer quantity) {
		String result = "";
		Integer qtyAvail = Inventory.get(product);

		if (quantity <= 0) {
			result = "Invalid quantity";

		}
		else if (qtyAvail == null) {
			result = "Not Available - We do not sell this product";
		}
		else if (qtyAvail < quantity) {
			result = "Not Available - Not enough items";
		}
		else {
			int id = orderID;
			orderID++;
			User user 		= getUser(name);
			Order newOrder 	= user.createOrder(id, product, quantity);

			allOrders.put(id, new OrderUserPair(newOrder, user));
			Inventory.put(product, Inventory.get(product) - quantity);

			result = "Your order has been placed, " + id + " " + name + " " + product + " " + quantity;
		}

		return result;
	}

	private static synchronized User getUser(String name) {
		User user = new User(name);
		int userIndex;
		if (users.contains(user)) {
			userIndex = users.indexOf(user);
			user = users.get(userIndex);

		} else {
			users.add(user);
		}

		return user;
	}
}

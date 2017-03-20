import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Thread.sleep;

public class Server {
/* Aria:
1 2 src/input/inventory.txt
localhost:8001
localhost:8002
*/

	final static String ACK = "[ACK]";
	final static String CMD = "[CMD]";
	private final static String REQ = "[REQ]";
	private final static String REL = "[REL]";



	static Map<Integer,ServerInfo> 		servers 		= new HashMap<>();
	static List<ServerInfo> 			waitingServers	= new ArrayList<>();
	static Map<String, Integer> 		Inventory 		= new ConcurrentHashMap<>();
    static List<User> 					users 			= new ArrayList<>();
	static Map<Integer, OrderUserPair> 	allOrders 		= new ConcurrentHashMap<>();
    static int orderID = 1;

	public static void main (String[] args) throws Exception {
//		System.out.println("Enter <server-id> <n> <inventory-path>");
//		System.out.println("Enter <ip-address>:<port-number>");

		//
		//Scan inputs
		Scanner sc 				= new Scanner(System.in);
		int myID 				= sc.nextInt();
		int numServer 			= sc.nextInt();
		String inventoryPath 	= sc.next();
		File f 					= new File(inventoryPath);
		int numAck 				= 0;
		int myPort 				= -1;
		String clientCommand = "";
		Scanner file_sc;

		for (int id = 1; id <= numServer; id++) {

			String ipAndPort 		= sc.next();
			String[] tokens 		= ipAndPort.split(":");
			String host				= tokens[0];
			Integer port 			= Integer.parseInt(tokens[1]);
			ServerInfo newServer 	= new ServerInfo(id, host, port);

			if (id == myID) myPort = port;

			servers.put(id, newServer);
		}


		//fill the inventory
		try{
			file_sc = new Scanner(f);

			while(file_sc.hasNext()){
				Inventory.put(file_sc.next(), file_sc.nextInt());
			}

		} catch(FileNotFoundException e){
			System.out.println("[ERROR] File Not Found.");
		}


		ServerSocket serverSocket = new ServerSocket(myPort);

		while(true){
			String request, response;
			Integer receivedID;
			ServerInfo myInfo = servers.get(myID);

			try (Socket externalSocket = serverSocket.accept()) {

				PrintWriter client_out = new PrintWriter(externalSocket.getOutputStream(), true);
				InputStream input = externalSocket.getInputStream();
				BufferedReader socket_in = new BufferedReader(new InputStreamReader((input)));

				request = socket_in.readLine();
				String[] reqTok = request.split(" ");
				System.out.println("before striping off the tag:");
				Arrays.stream(reqTok).forEach(s -> System.out.println(s +" "));
				request = request.substring(6);

				switch (reqTok[0]) {
					case CMD:
						client_out.println(ack(myID, request));        //send ack to client

						//send request to all other servers
						long myTimeStamp = System.currentTimeMillis() % 1000;
						myInfo.setTimeStamp(myTimeStamp);

						String reqMsg = REQ + " " + myInfo.getPort()
											+ " " + myInfo.getHost()
											+ " " + myInfo.getID()
											+ " " + myInfo.getTimeStamp();

						System.out.println("req msg: " + reqMsg);
						notifyOtherServers(myID, reqMsg);

						//insert self into waiting servers
						clientCommand = request;
						myInfo.setTimeStamp(myTimeStamp);
						servers.put(myID, myInfo);
						waitingServers.add(myInfo);

						numAck = 0;
						break;
					case REQ:
						System.out.println("In REQ: ");
						Arrays.stream(reqTok).forEach(s -> System.out.println(s +" "));
						Integer receivedPort = Integer.parseInt(reqTok[1]);
						String receivedHost = reqTok[2];
						receivedID = Integer.parseInt(reqTok[3]);
						Long receivedTimeStamp = Long.parseLong(reqTok[4]);

						//insert received request into waitingServers
						ServerInfo otherServer = servers.get(receivedID);
						otherServer.setTimeStamp(receivedTimeStamp);
						servers.put(myID, otherServer);
						waitingServers.add(otherServer);

						//send acknowledgement
						Socket otherSocket = new Socket(receivedHost, receivedPort);
						PrintWriter otherOut = new PrintWriter(otherSocket.getOutputStream());
						otherOut.println(ACK);
						otherOut.flush();
						break;
					case ACK:
						numAck = numAck + 1;

						//if received all acknowledgements and timestamp is smallest -> enterCS
						if (numAck == numServer - 1) {

							//check for smallest timeStamp
							ServerInfo firstServerInline = waitingServers.stream()
									.min(Comparator.comparing(ServerInfo::getTimeStamp)).get();

							if (firstServerInline.equals(myInfo)) {
								//enter CS and process command
								response = processCommand(clientCommand);

								client_out.println(response);
								client_out.flush();

								//release from CS
								//send request to all other servers
								waitingServers.remove(myInfo);

								String relMsg = REL + " " + myID + " " + clientCommand;
								notifyOtherServers(myID, relMsg);
							}
						}
						break;
					case REL:
						receivedID = Integer.parseInt(reqTok[1]);

						//delete the request from waiting servers
						ServerInfo releasedServer = servers.get(receivedID);
						waitingServers.remove(releasedServer);

						//if other server modified inventory or orders, perform the same action
						processCommand(reqTok[2]);

						//if received all acknowledgements and timestamp is smallest -> enterCS and then release
						if (numAck == numServer - 1) {

							//check for smallest timeStamp
							ServerInfo firstServerInline = waitingServers.stream()
									.min(Comparator.comparing(ServerInfo::getTimeStamp)).get();

							if (firstServerInline.equals(myInfo)) {
								//enter CS and process command
								response = processCommand(clientCommand);

								client_out.println(response);
								client_out.flush();

								//release from CS
								//send request to all other servers
								waitingServers.remove(myInfo);

								String relMsg = REL + " " + myInfo.getID() + " " + clientCommand;
								notifyOtherServers(myInfo.getID(), relMsg);
							}
						}
						break;
					default:
						System.out.println("[ERROR] Invalid message received.");
						break;
				}
			}
		}

	}

	private static void notifyOtherServers(int myID, String msg) throws IOException {
		for (int id = 1; id <= servers.size(); id++) {
			if (id == myID) continue;

			ServerInfo otherServer = servers.get(id);
			Socket otherSocket = new Socket(otherServer.getHost(), otherServer.getPort());
			PrintWriter servers_out = new PrintWriter(otherSocket.getOutputStream());

			servers_out.println(msg);
			servers_out.flush();
		}
	}

	private static String ack(int myID, String request) {
		return (ACK + " " +servers.get(myID).toString() + " received the request: " + request);
	}

	private static String processCommand(String req) throws IOException, InterruptedException {
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

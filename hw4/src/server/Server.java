package server;//EIDs: sm47767, ap44342

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import util.*;
import static client.Client.*;
import static server.ServerHandlers.*;

public final class Server {

	public final static String ACK = "[ACK]";
	public final static String CMD = "[CMD]";
	public final static String REQ = "[REQ]";
	public final static String REL = "[REL]";
	public final static String CMP = "[CMP]";
	public final static String END = "[END]";

	private static Map<Integer, ServerInfo> servers = new ConcurrentHashMap<>();
	private static Map<Long, ServerInfo> waitingServers = new ConcurrentHashMap<>();
	private static Map<String, Integer> Inventory = new ConcurrentHashMap<>();
	private static Map<Integer, OrderUserPair> allOrders = new ConcurrentHashMap<>();
	private static List<User> users = new ArrayList<>();
	private static int orderID = 1;
	private static int numServers;


	public static void main(String[] args) throws Exception {

		//
		//Scan inputs
		Scanner sc            = new Scanner(System.in);
		int     myID          = sc.nextInt();
		int     totServers    = sc.nextInt();
		String  inventoryPath = sc.next();
		File    f             = new File(inventoryPath);
		numServers = totServers;
		int myPort = extractServers(sc, myID);

		//		System.out.println("[DEBUG] my id: " + myID);
		//		System.out.println("[DEBUG] numServer: " + numServers);
		//		System.out.println("[DEBUG] inventory path: " + inventoryPath);
		maybeTag.apply("[INT] Initialization.");
		loadInventory(f);

		ServerSocket serverSocket = new ServerSocket(myPort);
		Socket       socket;

		while (true) {
			ServerInfo myInfo = servers.get(myID);
			socket = serverSocket.accept();
			Socket externalSocket = socket;

			serverHandler.apply(externalSocket).accept(externalSocket, myInfo);

		}
	}

	static synchronized ServerInfo getFirstServerInline() {
		Collection<ServerInfo> values = waitingServers.values();
		return values.stream()
		             .filter(ServerInfo::isAvail)
		             .min(Comparator.comparing(ServerInfo::getTimeStamp)).get();
	}

	static synchronized void addWaitingServer(ServerInfo otherServer) {
		ServerInfo newServerTask = ServerInfo.dupServer(otherServer);

		waitingServers.put(otherServer.getTimeStamp(), newServerTask);
	}

	static synchronized ServerInfo removeWaitingServer(Long timeStamp) {
		return waitingServers.remove(timeStamp);
	}

	static synchronized void removeAllWaiting(ServerInfo s) {
		waitingServers.forEach((timeStamp, server) -> {
			if (Objects.equals(server.getID(), s.getID()))
				waitingServers.remove(timeStamp);
		});
	}

	private static int extractServers(Scanner sc, int myID) {
		int myPort = 0;

		for (int id = 1; id <= numServers; id++) {

			String     ipAndPort = sc.next();
			String[]   tokens    = ipAndPort.split(":");
			String     host      = tokens[0];
			Integer    port      = Integer.parseInt(tokens[1]);
			ServerInfo newServer = new ServerInfo(id, host, port);

			if (id == myID) myPort = port;

			servers.put(id, newServer);
		}

		return myPort;
	}

	private static void loadInventory(File f) {
		Scanner file_sc;
		try {
			file_sc = new Scanner(f);

			while (file_sc.hasNext()) {
				Inventory.put(file_sc.next(), file_sc.nextInt());
			}

		}
		catch (FileNotFoundException e) {
			System.out.println("[ERROR] File Not Found.");
		}
	}

	static void notifyServers(int myID, String msg) {

		for (int id = 1; id <= servers.size(); id++) {

			if (id == myID) continue;
			if (servers.get(id).isCrashed()) continue;

			ServerInfo otherServer = servers.get(id);
			Socket     otherSocket;

			try {

				otherSocket = new Socket(otherServer.getHost(), otherServer.getPort());

				PrintWriter servers_out = SocketStreams.getOutStream.apply(otherSocket).get();

				servers_out.println(msg);
				servers_out.flush();

			}
			catch (IOException e) {
				//				System.out.println("IO Exception while waiting for an input:");
				//				killThisServer(otherServer.getID());
				//				continue;
			}

		}

	}

	static void broadcastToOtherServers(int myID, String msg) {

		for (int id = 1; id <= servers.size(); id++) {

			if (id == myID) continue;
			if (servers.get(id).isCrashed()) continue;

			ServerInfo otherServer = servers.get(id);
			Socket     otherSocket;

			try {

				otherSocket = new Socket(otherServer.getHost(), otherServer.getPort());
				otherSocket.setSoTimeout(CONNECTION_TIMEOUT);

				PrintWriter    servers_out = SocketStreams.getOutStream.apply(otherSocket).get();
				BufferedReader servers_in  = SocketStreams.getInStream.apply(otherSocket).get();

				servers_out.println(msg);
				servers_out.flush();


				String respLine;

				while ((respLine = servers_in.readLine()) != null) {

					String[] respTok = respLine.split(" ");
					if (respTok[0].equals(ACK)) break;
				}

			}
			catch (IOException e) {
				killThisServer(otherServer.getID());
			}

		}

	}

	static String ack(int myID, String request) {
		return (ACK + " " + servers.get(myID).toString() + " received the request: " + request);
	}

	static String processCommand(String req) {
		String[] tokens = req.split(" ");
		String   result;
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
			default:
				result = "ERROR: No such command";
				break;
		}
		return result;
	}

	static void killThisServer(int serverId) {
		servers.get(serverId).killServer();
	}

	private static String listAllProducts() {
		String   result = "";
		Iterator it     = getInventoryIterator();
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
		User   user   = new User(userName);
		User   foundUser;
		int    userIndex;
		if (users.contains(user)) {
			userIndex = users.indexOf(user);
			foundUser = users.get(userIndex);
			for (int i = 0; i < foundUser.getOrders().size(); i++) {
				Order   order       = foundUser.getOrders().get(i);
				Integer orderID     = order.getOrderID();
				String  productName = order.getProduct();
				Integer quantity    = order.getQuantity();
				result = result + orderID + " " + productName + " " + quantity + "\n";
			}
		}
		else {
			result = "No order found for " + userName;
		}
		return result;

	}

	private static synchronized String cancelOrder(Integer id) {
		String        result;
		OrderUserPair orderUserPair;

		if ((orderUserPair = allOrders.get(id)) != null) {
			Order order = orderUserPair.getOrder();

			String product  = order.getProduct();
			int    quantity = order.getQuantity();

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
		String  result;
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
			User  user     = getUser(name);
			Order newOrder = user.createOrder(id, product, quantity);

			allOrders.put(id, new OrderUserPair(newOrder, user));
			Inventory.put(product, Inventory.get(product) - quantity);

			result = "Your order has been placed, " + id + " " + name + " " + product + " " + quantity;
		}

		return result;
	}

	private static synchronized User getUser(String name) {
		User user = new User(name);
		int  userIndex;
		if (users.contains(user)) {
			userIndex = users.indexOf(user);
			user = users.get(userIndex);

		}
		else {
			users.add(user);
		}

		return user;
	}

	static Map<Integer, ServerInfo> getAllServers() {
		return servers;
	}
}

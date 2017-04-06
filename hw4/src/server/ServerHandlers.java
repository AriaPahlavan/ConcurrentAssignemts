package server;

import util.ServerInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.lang.Thread.sleep;

/**
 * Created by Aria Pahlavan on 3/24/17.
 */
class ServerHandlers {
	private static BiConsumer<Socket, String[]> clientAckHandler = (socket, idAndReq) -> {
		PrintWriter client_out = SocketStreams.getOutStream.apply(socket).get();
		try {


			while (!Thread.currentThread().isInterrupted()) {
//				System.out.println("[DEBUG] Ack at " + Clock.systemUTC().millis());

				//send ack to client
				client_out.println(Server.ack(Integer.parseInt(idAndReq[0]), idAndReq[1]));
				sleep(70);
			}

		}
		catch (InterruptedException ignored) {}
	};

	private static Function<String, BiConsumer<Socket, ServerInfo>> clientMsgHandler = req -> ((externalSocket, myInfo) -> {
		int         myID       = myInfo.getID();
		String      response;
		String      request    = req;
		PrintWriter client_out = null;

		request = request.substring(6);

		try {

			client_out = new PrintWriter(externalSocket.getOutputStream(), true);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		String[] idAndReq           = new String[]{String.valueOf(myID), request};
		Runnable runnable           = () -> clientAckHandler.accept(externalSocket, idAndReq);
		Thread   clientAcknowledger = new Thread(runnable);

//		System.out.println("[DEBUG] Running acknowledger at " + Clock.systemUTC().millis());
		clientAcknowledger.start();

		//send request to all other servers

		long myTimeStamp = System.currentTimeMillis() % 1000000;
		myInfo.setTimeStamp(myTimeStamp);

		String reqMsg = Server.REQ
		                + " " + myInfo.getPort()
		                + " " + myInfo.getHost()
		                + " " + myInfo.getID()
		                + " " + myInfo.getTimeStamp();


//		System.out.println("[DEBUG] Broadcasting request at " + Clock.systemUTC().millis());
		Server.broadcastToOtherServers(myID, reqMsg);


		//insert self into waiting servers
		myInfo.setTimeStamp(myTimeStamp);
		Server.getAllServers().put(myID, myInfo);
		Server.addWaitingServer(myInfo);


		while (true) {
			//check for smallest timeStamp
			ServerInfo firstServerInline = Server.getFirstServerInline();


			if (firstServerInline.equals(myInfo)) {
				//enter CS and process command
//				System.out.println("[DEBUG] Entering CS at " + Clock.systemUTC().millis());
				response = Server.processCommand(request);


				clientAcknowledger.interrupt();

				try {
					clientAcknowledger.join();
				}
				catch (InterruptedException ignored) {}

//				System.out.println("[DEBUG] Responding to client at " + Clock.systemUTC().millis());
				client_out.println(response);
				client_out.println(Server.END);
				client_out.flush();

				//release from CS
				//send release to all other servers
				Server.removeWaitingServer(myInfo.getTimeStamp());

				String relMsg = Server.REL + " " + myInfo.getTimeStamp() + " " + request;
				Server.notifyServers(myID, relMsg);
				break;
			}
			else {
				//check if server first in line is alive
				try {
					Socket firstServer = new Socket(firstServerInline.getHost(), firstServerInline.getPort());
				}
				catch (IOException e) {
					Server.removeAllWaiting(firstServerInline);
					Server.killThisServer(firstServerInline.getID());
				}
			}
		}
	});

	private static Function<Socket, Optional<String>> maybeRequest = socket -> {
		String request = null;

		try {
//			System.out.println("[DEBUG] received message at " + Clock.systemUTC().millis());
			request = SocketStreams.getInStream.apply(socket).get().readLine();
		}
		catch (Exception ignored) {}

		return Optional.ofNullable(request);
	};

	static Function<String, Optional<String>> maybeTag = request -> {

		String[] reqTok = request.split(" ");
		String   tag    = reqTok[0];

		return Optional.ofNullable(tag);
	};

	private static BiConsumer<String, ServerInfo> serverMsgHandler = (request, myInfo) -> {
		int     myID = myInfo.getID();
		Integer receivedID;

		String[] reqTok = request.split(" ");
		request = request.substring(6);

		switch (reqTok[0]) {
			case Server.REQ:
				Integer receivedPort = Integer.parseInt(reqTok[1]);
				String receivedHost = reqTok[2];
				receivedID = Integer.parseInt(reqTok[3]);
				Long receivedTimeStamp = Long.parseLong(reqTok[4]);

				//insert received request into waitingServers
				ServerInfo otherServer = Server.getAllServers().get(receivedID);
				otherServer.setTimeStamp(receivedTimeStamp);
				Server.getAllServers().put(receivedID, otherServer);
				Server.addWaitingServer(otherServer);

				//send acknowledgement
				Socket otherSocket;

				try {

					otherSocket = new Socket(receivedHost, receivedPort);
					PrintWriter otherOut;
					otherOut = new PrintWriter(otherSocket.getOutputStream());

					otherOut.println(Server.ACK + "server " + myInfo + " received request: " + request);
					otherOut.flush();

				}
				catch (IOException e) {
					e.printStackTrace();
				}

				break;
			case Server.REL:
				receivedTimeStamp = Long.parseLong(reqTok[1]);

				//delete the request from waiting servers
				Server.removeWaitingServer(receivedTimeStamp);

				//update inventory or orders
				int i;

				for (i = 0; i < request.length(); i++) {
					if (request.toCharArray()[i] == ' ')
						break;

				}

				String command = request.substring(i + 1);

				Server.processCommand(command);

				String compMsg = Server.CMP + " " + receivedTimeStamp + " " + command;
				Server.notifyServers(myID, compMsg);
				break;
			case Server.CMP:
				receivedTimeStamp = Long.parseLong(reqTok[1]);

				//check if the server is still in waitingList
				if (Server.removeWaitingServer(receivedTimeStamp) != null) {
					//update inventory or orders
					int j;

					for (j = 0; j < request.length(); j++) {
						if (request.toCharArray()[j] == ' ')
							break;

					}

					String completedCommand = request.substring(j + 1);
					Server.processCommand(completedCommand);
				}
				break;
			case Server.ACK:
				break;
			default:
				break;
		}
	};

	private static Function<Optional<String>, BiConsumer<Socket, ServerInfo>> handler = optionReq -> (externalSocket, myInfo) -> {
		optionReq.ifPresent(request -> maybeTag.apply(request).ifPresent(tag -> {
			try {
				if (tag.equals(Server.CMD)) {
					//Spawn a new thread to handle incoming messages!
					BiConsumer<Socket, ServerInfo> clientHandlerWithReq = clientMsgHandler.apply(request);
					Runnable                       clientTask           = () -> clientHandlerWithReq.accept(externalSocket, myInfo);
					new Thread(clientTask).start();
				}

				if (!tag.equals(Server.CMD)) {
					externalSocket.close();
					Runnable serverTask = () -> serverMsgHandler.accept(request, myInfo);
					new Thread(serverTask).start();
				}
			}
			catch (IOException ignored) {}

		}));
	};

	static Function<Socket, BiConsumer<Socket, ServerInfo>> serverHandler = handler.compose(maybeRequest);
}

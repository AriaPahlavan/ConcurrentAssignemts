//EIDs: sm47767, ap44342

import java.io.*;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Function;

public final class SocketStreams {

	final static Function<Socket, Optional<PrintWriter>> getOutStream = socket -> {
		PrintWriter socket_out = null;

		try {

			socket_out = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
		}

		return Optional.ofNullable(socket_out);
	};

	final static Function<Socket, Optional<BufferedReader>> getInStream = socket -> {
		BufferedReader socket_in = null;

		try {

			InputStream input = socket.getInputStream();
			socket_in = new BufferedReader(new InputStreamReader((input)));

		} catch (IOException e) {
		}

		return Optional.ofNullable(socket_in);
	};
}

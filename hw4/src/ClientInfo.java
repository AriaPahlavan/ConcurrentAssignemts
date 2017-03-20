import java.io.PrintWriter;

/**
 * Created by aria on 3/18/17.
 */
class ClientInfo {
	private Pair<PrintWriter, String> pair;

	public ClientInfo(PrintWriter client_out, String clientCommand) {
		this.pair = new Pair<>(client_out, clientCommand);
	}

	PrintWriter getOutputStream() {
		return pair.getLeft();
	}

	String getCommand() {
		return pair.getRight();
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ClientInfo)) return false;

		ClientInfo that = (ClientInfo) o;

		return pair != null ? pair.equals(that.pair) : that.pair == null;
	}

	@Override
	public int hashCode() {
		return pair != null ? pair.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "<" + pair.getLeft() + ", " + pair.getRight() + ">";
//				+ " is " + (isCrashed ? "" : "not") + " crashed.";
	}
}

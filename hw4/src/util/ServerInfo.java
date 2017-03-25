package util;//EIDs: sm47767, ap44342

public class ServerInfo {
	private Pair<String, Integer> pair;
	private Long timeStamp;
	private Integer port;
	private Integer serverID;
	private boolean isCrashed = false;

	public ServerInfo() {}

	public ServerInfo(int serverID, String IP, Integer port) {
		this.pair = new Pair<>(IP, port);
		this.serverID = serverID;
	}

	public ServerInfo(String IP, Integer port) {
		this.pair = new Pair<>(IP, port);
	}

	public Long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Integer getID() {
		return serverID;
	}

	public String getHost() {
		return pair.getLeft();
	}

	public Integer getPort() {
		return pair.getRight();
	}

	public boolean isAvail() {
		return !isCrashed;
	}

	public boolean isCrashed() {
		return isCrashed;
	}

	public void killServer() {
		isCrashed = true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ServerInfo)) return false;

		ServerInfo that = (ServerInfo) o;

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

	public static ServerInfo dupServer(ServerInfo s) {
		ServerInfo newServer = new ServerInfo(s.getID(), s.getHost(), s.getPort());
		newServer.setTimeStamp(s.getTimeStamp());
		return newServer;
	}
}

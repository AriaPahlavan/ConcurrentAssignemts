/**
 * Created by Sharmistha on 3/18/2017.
 */
public class ServerInfo {
    long timeStamp;
    int port;
    int serverID;

    public ServerInfo(int id, int port){
        this.serverID = id;
        this.port = port;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getServerID() {
        return serverID;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getPort() {
        return port;
    }
}

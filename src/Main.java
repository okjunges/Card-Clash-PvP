import client.ClientFrame;
import common.ServerInfo;
import server.Server;

public class Main {
    public static void main(String[] args) {
        String serverAddress = ServerInfo.getInstance().getIP();
        int port = ServerInfo.getInstance().getPORT();

        new ClientFrame();
        new ClientFrame();
        new ClientFrame();
        new ClientFrame();
        Server server = new Server(port);
    }
}
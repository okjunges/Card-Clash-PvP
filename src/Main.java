import client.Client;
import common.ServerInfo;
import server.Server;

public class Main {
    public static void main(String[] args) {
        String serverAddress = ServerInfo.getInstance().getIP();
        int port = ServerInfo.getInstance().getPORT();

        Client client = new Client(serverAddress, port);
        Client client1 = new Client(serverAddress, port);
        Server server = new Server(port);
    }
}
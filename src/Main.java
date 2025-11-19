import client.Client;
import server.Server;

public class Main {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 54321;

        Client client = new Client(serverAddress, port);
        Client client1 = new Client(serverAddress, port);
        Server server = new Server(port);
    }
}
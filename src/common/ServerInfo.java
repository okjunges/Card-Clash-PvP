package common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ServerInfo {
    private final String IP;
    private final int PORT;

    private ServerInfo() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("server.txt"));
            this.IP = lines.get(0).trim();
            this.PORT = Integer.parseInt(lines.get(1).trim());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load server.txt", e);
        }
    }

    public String getIP() {
        return IP;
    }

    public int getPORT() {
        return PORT;
    }
    private static class LazyHolder {
        public static final ServerInfo INSTANCE = new ServerInfo();
    }
    public static ServerInfo getInstance() {
        return LazyHolder.INSTANCE;
    }
}
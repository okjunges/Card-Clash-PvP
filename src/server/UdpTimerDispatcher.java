package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class UdpTimerDispatcher {
    private final DatagramSocket socket;

    public UdpTimerDispatcher(DatagramSocket socket) { this.socket = socket; }

    public void send(Room room, int remainSec) {
        String payload =
                "TIMER|" +
                room.getRoomName() + "|" +
                room.getTurnNumber() + "|" +
                room.getCurrentTurnUid() + "|" +
                remainSec;
        sendTo(room.getP1Udp(), payload);
        sendTo(room.getP2Udp(), payload);
    }

    private void sendTo(InetSocketAddress target, String payload) {
        if (target == null) return;

        try {
            byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, target);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("[UDP] send failed to " + target + " : " + e.getMessage());
        }
    }
}
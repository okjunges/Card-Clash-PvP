package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

// UDP 통신 구현 클래스
// 클라이언트로부터 받은 정보를 저장한 서버에 저장된 소켓에 턴 진행 시간을 정해진 양식에 맞춰 패킷을 만든 후 게임 방에 저장된 목적지 주소로 턴 진행 시간을 전송한다
// 외부 개념 참고
// https://rainbow97.tistory.com/entry/JAVA-18-4-UDP-%EB%84%A4%ED%8A%B8%EC%9B%8C%ED%82%B9
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
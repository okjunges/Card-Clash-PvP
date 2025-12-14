package server;

import common.Message;

import java.net.InetSocketAddress;
import java.net.Socket;

public interface Session {
    String getUid();
    void send(Message msg);
    InetSocketAddress getInetSocketAddress();
}
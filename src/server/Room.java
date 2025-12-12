package server;

import common.Card;
import common.Message;
import common.State;

public class Room {
    private String roomName;
    private Server.ClientHandler player1;
    private Server.ClientHandler player2;
    private State p1State;
    private State p2State;

    Room(String roomName, Server.ClientHandler player1) {
        this.roomName = roomName;
        this.player1 = player1;
        this.p1State = new State(player1.getUid(), 30, 3, 0);
    }
    public void enterRoom(Server.ClientHandler player2) {
        this.player2 = player2;
        this.p2State = new State(player2.getUid(), 30, 3, 0);
    }
    public boolean isReady() {
        return player1 != null && player2 != null;
    }
    public State getStateOf(Server.ClientHandler handler) {
        if (handler == player1) return p1State;
        else if (handler == player2) return p2State;
        else return null;
    }
    public State getOpponentStateOf(Server.ClientHandler handler) {
        if (handler == player1) return p2State;
        else if (handler == player2) return p1State;
        else return null;
    }
    public boolean applyCard(Card card, Server.ClientHandler caster) {
        State me = getStateOf(caster);
        State enemy = getOpponentStateOf(caster);
        return card.executeCard(me, enemy);
    }

    public String getRoomName() { return roomName; }
    public State getP1State() { return p1State; }
    public State getP2State() { return p2State; }
    public Server.ClientHandler getPlayer1() { return player1; }
    public Server.ClientHandler getPlayer2() { return player2; }

    public void broadcasting(Message msg) {
        if (player1 != null) player1.send(msg);
        if (player2 != null) player2.send(msg);
    }
}
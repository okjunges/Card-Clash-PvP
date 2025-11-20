package common;

import java.io.Serializable;

public class State implements Serializable {
    private String name;
    private int hp;
    private int cost;
    private int shield;

    public State(String name, int hp, int cost, int shield) {
        this.name = name;
        this.hp = hp;
        this.cost = cost;
        this.shield = shield;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getShield() {
        return shield;
    }

    public void setShield(int shield) {
        this.shield = shield;
    }
}
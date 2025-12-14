package common;

import java.io.Serializable;

public class State implements Serializable {
    private String name;
    private int hp;
    private int cost;
    private int shield;
    private int bonusDamage;
    private int reflectDamage;

    public State(String name, int hp, int cost, int shield) {
        this.name = name;
        this.hp = hp;
        this.cost = cost;
        this.shield = shield;
        this.bonusDamage = 0;
        this.reflectDamage = 0;
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

    public void addCoat(int addCost) { this.cost += addCost; }

    public int getShield() {
        return shield;
    }

    public void setShield(int shield) {
        this.shield = shield;
    }

    public void addShield(int shield) {
        this.shield += shield;
    }

    public void setBonusDamage(int bonusDamage) { this.bonusDamage = bonusDamage; }

    public int getBonusDamage() { return bonusDamage; }

    public void resetBonusDamage() { this.bonusDamage = 0; }

    public void setReflectDamage(int reflectDamage) { this.reflectDamage = reflectDamage; }

    public int getReflectDamage() { return reflectDamage; }

    public void resetReflectDamage() { this.reflectDamage = 0; }
}
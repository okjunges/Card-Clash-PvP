package common;

public class CardIronWall extends Card {
    public CardIronWall() {
        // 3코스트 감소, 방어 8
        this.cardName = "Iron Wall";
        this.cost = 3;
        this.damage = 0;
        this.shield = 8;
    }

    @Override
    public void changeState(State attacker, State target) {
        attacker.addShield(shield);
    }

    @Override
    public void drawCard() {

    }
}
package common;

public class CardDefend extends Card {
    public CardDefend() {
        // 2코스트 감소, 방어 5
        this.cardName = "Defend";
        this.cost = 2;
        this.damage = 0;
        this.shield = 5;
    }

    @Override
    public void changeState(State attacker, State target) {
        attacker.addShield(shield);
    }

    @Override
    public void drawCard() {

    }
}
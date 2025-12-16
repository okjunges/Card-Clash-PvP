package common;

public class CardChargeUp extends Card {
    public CardChargeUp() {
        // 0코스트 감소, 코스트 1 획득, 손패 드로우(클라이언트)
        this.cardName = "Charge Up";
        this.cost = 0;
        this.damage = 0;
        this.shield = 0;
    }

    @Override
    public void changeState(State attacker, State target) {
        attacker.addCoat(1);
    }
}

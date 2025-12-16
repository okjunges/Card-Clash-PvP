package common;

public class CardAdrenalineRush extends Card {
    public CardAdrenalineRush() {
        // 1코스트 감소, hp 2 감소
        this.cardName = "Adrenaline Rush";
        this.cost = 1;
        this.damage = 0;
        this.shield = 0;
    }

    @Override
    public void changeState(State attacker, State target) {
        applyDamageWithShield(attacker, 2);
    }
}

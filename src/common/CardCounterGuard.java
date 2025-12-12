package common;

public class CardCounterGuard extends Card {
    public CardCounterGuard() {
        // 4코스트 감소, 방어 6, 다음 공격 받으면 상대에게 3 반사
        this.cardName = "Counter Guard";
        this.cost = 4;
        this.damage = 0;
        this.shield = 6;
    }

    @Override
    public void changeState(State attacker, State target) {
        attacker.addShield(shield);
        attacker.setReflectDamage(3);
    }

    @Override
    public void drawCard() {

    }
}

package common;

public class CardBonus extends Card {
    public CardBonus() {
        // 4코스트 감소, 방어 6, 다음 공격 받으면 상대에게 3 반사
        this.cardName = "Bonus!";
        this.cost = 0;
        this.damage = 3;
        this.shield = 3;
    }

    @Override
    public void changeState(State attacker, State target) {
        int damageLeft = damage + attacker.getBonusDamage();
        applyDamageWithShield(target, damageLeft);
        attacker.addShield(shield);
        reflectCard(attacker, target);
    }

    @Override
    public void drawCard() {

    }
}
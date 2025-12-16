package common;

public class CardHeavyBlow extends Card {
    public CardHeavyBlow() {
        // 2코스트 감소, 데미지 8
        this.cardName = "Heavy Blow";
        this.cost = 2;
        this.damage = 8;
        this.shield = 0;
    }

    @Override
    public void changeState(State attacker, State target) {
        int damageLeft = damage + attacker.getBonusDamage();

        applyDamageWithShield(target, damageLeft);

        // 공격 후 상대방의 반사가 있을 때 반사 적용
        reflectCard(attacker, target);
    }
}

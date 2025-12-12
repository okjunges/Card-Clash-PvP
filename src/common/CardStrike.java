package common;

public class CardStrike extends Card {
    public CardStrike() {
        // 1코스트 감소, 데미지 4
        this.cardName = "Strike";
        this.cost = 1;
        this.damage = 4;
        this.shield = 0;
    }

    @Override
    public void changeState(State attacker, State target) {
        int damageLeft = damage + attacker.getBonusDamage();

        applyDamageWithShield(target, damageLeft);

        // 공격 후 상대방의 반사가 있을 때 반사 적용
        reflectCard(attacker, target);
    }

    @Override
    public void drawCard() {

    }
}

package common;

public class CardWeaknessStrike extends Card {
    public CardWeaknessStrike() {
        // 5코스트 감소, 데미지 6, 상대에게 취약 1턴(이번 턴 데미지 +2, 추가 데미지는 누적 불가능)
        this.cardName = "Weakness Strike";
        this.cost = 5;
        this.damage = 6;
        this.shield = 0;
    }

    @Override
    public void changeState(State attacker, State target) {
        int damageLeft = damage + attacker.getBonusDamage();

        applyDamageWithShield(target, damageLeft);

        // 공격 후 상대방의 반사가 있을 때 반사 적용
        reflectCard(attacker, target);

        attacker.setBonusDamage(2);
    }

    @Override
    public void drawCard() {

    }
}
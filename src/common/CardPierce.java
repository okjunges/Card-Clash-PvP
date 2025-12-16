package common;

public class CardPierce extends Card {
    public CardPierce() {
        // 2코스트 감소, 데미지 5, 방어 무시
        this.cardName = "Pierce";
        this.cost = 2;
        this.damage = 5;
        this.shield = 0;
    }

    @Override
    public void changeState(State attacker, State target) {
        int damageLeft = damage + attacker.getBonusDamage();

        target.setHp(Math.max(0, target.getHp() - damageLeft));

        // 공격 후 상대방의 반사가 있을 때 반사 적용
        reflectCard(attacker, target);
    }
}
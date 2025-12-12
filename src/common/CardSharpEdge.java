package common;

public class CardSharpEdge extends Card {
    public CardSharpEdge() {
        // 3코스트 감소, 이번 턴 데미지 +2, 추가 데미지는 누적 불가능
        this.cardName = "Sharp Edge";
        this.cost = 3;
        this.damage = 0;
        this.shield = 0;
    }

    @Override
    public void changeState(State attacker, State target) {
        attacker.setBonusDamage(2);
    }

    @Override
    public void drawCard() {

    }
}

package common;

import java.io.Serializable;

abstract public class Card implements Serializable {
    String cardName;
    int cost;
    int damage;
    int shield;

    public String getCardName() { return cardName; }

    // 카드 효과
    public boolean executeCard(State attacker, State target) {
        if (attacker.getCost() < cost) {
            System.err.println("코스트 부족 문제");
            return false;
        }
        attacker.setCost(attacker.getCost() - cost);
        changeState(attacker, target);
        return true;
    }

    public void reflectCard(State attacker, State target) {
        if (target.getReflectDamage() > 0) {
            int damage = target.getReflectDamage();

            target.resetReflectDamage();
            applyDamageWithShield(attacker, damage);
        }
    }

    public void applyDamageWithShield(State target, int damage) {
        // 실드 처리
        if (target.getShield() > 0) {
            int shield = target.getShield();

            if (shield >= damage) {
                target.setShield(shield - damage);
                damage = 0;
            } else {
                damage -= shield;
                target.setShield(0);
            }
        }

        // 남은 데미지 처리
        if (damage > 0) {
            target.setHp(Math.max(0, target.getHp() - damage));
        }
    }

    abstract public void changeState(State attacker, State target);
}
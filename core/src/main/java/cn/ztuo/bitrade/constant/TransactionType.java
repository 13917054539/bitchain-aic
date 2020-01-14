package cn.ztuo.bitrade.constant;

import com.fasterxml.jackson.annotation.JsonValue;

import cn.ztuo.bitrade.core.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TransactionType implements BaseEnum {
    RECHARGE("充值"),
    WITHDRAW("提现"),
    TRANSFER_ACCOUNTS("转账"),
    EXCHANGE("币币交易"),
    OTC_BUY("法币买入"),
    OTC_SELL("法币卖出"),
    ACTIVITY_AWARD("活动奖励"),
    PROMOTION_AWARD("推广奖励"),
    DIVIDEND("分红"),
    VOTE("投票"),
    ADMIN_RECHARGE("人工充值"),
    MATCH("配对"),
    RETURN_CURRENCY("还币"),
    BORROW_MONEY("借币"),
    COMMUNITY_REWARDS("社区奖励"),
    DIRECT_PUSH_AWARD("直推奖励"),
    INFINITE_REWARD("级别奖励"),
    HIERARCHICAL_AWARD("层级奖"),
    KEEPING_MONEY_TO_EARN_INTEREST("持币生息"),
    JOINING_THE_CONTRACT("加入合约"),
    RESCISSION_OF_CONTRACT("解除合约"),
    DEDUCTION_OF_WITHDRAWAL_MONEY("提币扣除"),
    REJECTION_OF_WITHDRAWAL_OF_CURRENCY("提币驳回"),
    REGISTRATION_AWARD("注册奖励"),
    SIGN_IN_AWARD("签到奖励"),
    CMB_ROLL_OUT("CMB转出"),
    LEVEL_AWARD("级别奖"),
    QUOTA_TRANSFER("减少额度"),
    INTEREST_RATE_INCREASE_AWARD("释放奖"),
    INCREASED_QUOTA("增加额度"),
    CTO_SIGN_IN_AWARD("CTO签到奖励"),
    TRANSFORMATION("转换数量"),
    LOWER_SHELF("下架"),
    UPPER_SHELF("上架"),
    COIN_RELEASE("放币"),
    CANCELLATION_OF_ORDER("取消订单"),
    BUYING_COINS("卖币"),
    CTORECOMMENDATIONAWARD("CTO推荐奖"),
    RUSH_TO_PURCHASE("抢购CAL"),
    RUSH_TO_PURCHASE_DEDUCT("抢购扣除"),
    DIRECT_PUSH_PANIC_BUYING_AWARD("直推抢购奖励"),
    REWARDS_TO_RELEASE("奖励释放"),
    FROZEN("冻结"),
    UNFREEZE("解冻"),
    Robot_end_retreat("机器人结束退回");
    ;

    private String cnName;
    @Override
    @JsonValue
    public int getOrdinal() {
        return this.ordinal();
    }

    public static TransactionType valueOfOrdinal(int ordinal){
        switch (ordinal){
            case 0:return RECHARGE;
            case 1:return WITHDRAW;
            case 2:return TRANSFER_ACCOUNTS;
            case 3:return EXCHANGE;
            case 4:return OTC_BUY;
            case 5:return OTC_SELL;
            case 6:return ACTIVITY_AWARD;
            case 7:return PROMOTION_AWARD;
            case 8:return DIVIDEND;
            case 9:return VOTE;
            case 10:return ADMIN_RECHARGE;
            case 11:return MATCH;
            case 12:return RETURN_CURRENCY;
            case 13:return BORROW_MONEY;
            case 14:return COMMUNITY_REWARDS;
            case 15:return DIRECT_PUSH_AWARD;
            case 16:return INFINITE_REWARD;
            case 17:return HIERARCHICAL_AWARD;
            case 18:return KEEPING_MONEY_TO_EARN_INTEREST;
            case 19:return JOINING_THE_CONTRACT;
            case 20:return RESCISSION_OF_CONTRACT;
            case 21:return DEDUCTION_OF_WITHDRAWAL_MONEY;
            case 22:return REJECTION_OF_WITHDRAWAL_OF_CURRENCY;
            case 23:return REGISTRATION_AWARD;
            case 24:return SIGN_IN_AWARD;
            case 25:return CMB_ROLL_OUT;
            case 26:return LEVEL_AWARD;
            case 27:return QUOTA_TRANSFER;
            case 28:return INTEREST_RATE_INCREASE_AWARD;
            case 29:return INCREASED_QUOTA;
            case 30:return CTO_SIGN_IN_AWARD;
            case 31:return TRANSFORMATION;
            case 32:return LOWER_SHELF;
            case 33:return UPPER_SHELF;
            case 34:return COIN_RELEASE;
            case 35:return CANCELLATION_OF_ORDER;
            case 36:return BUYING_COINS;
            case 37:return CTORECOMMENDATIONAWARD;
            case 38:return RUSH_TO_PURCHASE;
            case 39:return RUSH_TO_PURCHASE_DEDUCT;
            case 40:return DIRECT_PUSH_PANIC_BUYING_AWARD;
            case 41:return REWARDS_TO_RELEASE;
            case 42:return FROZEN;
            case 43:return UNFREEZE;
            case 44:return Robot_end_retreat;
            default:return null;
        }
    }
    public static int parseOrdinal(TransactionType ordinal) {
        if (TransactionType.RECHARGE.equals(ordinal)) {
            return 0;
        } else if (TransactionType.WITHDRAW.equals(ordinal)) {
            return 1;
        } else if (TransactionType.TRANSFER_ACCOUNTS.equals(ordinal)) {
            return 2;
        } else if (TransactionType.EXCHANGE.equals(ordinal)) {
            return 3;
        } else if (TransactionType.OTC_BUY.equals(ordinal)) {
            return 4;
        } else if (TransactionType.OTC_SELL.equals(ordinal)) {
            return 5;
        } else if (TransactionType.ACTIVITY_AWARD.equals(ordinal)) {
            return 6;
        }else if (TransactionType.PROMOTION_AWARD.equals(ordinal)) {
            return 7;
        }else if (TransactionType.DIVIDEND.equals(ordinal)) {
            return 8;
        }else if (TransactionType.VOTE.equals(ordinal)) {
            return 9;
        }else if (TransactionType.ADMIN_RECHARGE.equals(ordinal)) {
            return 10;
        }else if(TransactionType.MATCH.equals(ordinal)){
            return 11;
        } else if(TransactionType.RETURN_CURRENCY.equals(ordinal)){
            return 12;
        }else if(TransactionType.BORROW_MONEY.equals(ordinal)){
            return 13;
        }else if(TransactionType.COMMUNITY_REWARDS.equals(ordinal)) {
            return 14;
        }else if(TransactionType.DIRECT_PUSH_AWARD.equals(ordinal)) {
            return 15;
        }else if (TransactionType.INFINITE_REWARD.equals(ordinal)){
            return 16;
        }else if (TransactionType.HIERARCHICAL_AWARD.equals(ordinal)){
            return 17;
        }else if (TransactionType.KEEPING_MONEY_TO_EARN_INTEREST.equals(ordinal)){
            return 18;
        }  else if (TransactionType.JOINING_THE_CONTRACT.equals(ordinal)){
            return 19;
        } else if (TransactionType.RESCISSION_OF_CONTRACT.equals(ordinal)){
            return 20;
        } else if (TransactionType.DEDUCTION_OF_WITHDRAWAL_MONEY.equals(ordinal)){
            return 21;
        } else if (TransactionType.REJECTION_OF_WITHDRAWAL_OF_CURRENCY.equals(ordinal)){
            return 22;
        } else if (TransactionType.REGISTRATION_AWARD.equals(ordinal)){
            return 23;
        }else if (TransactionType.CMB_ROLL_OUT.equals(ordinal)){
            return 25;
        }else if (TransactionType.LEVEL_AWARD.equals(ordinal)){
            return 26;
        }else if (TransactionType.QUOTA_TRANSFER.equals(ordinal)){
            return 27;
        }else if (TransactionType.INTEREST_RATE_INCREASE_AWARD.equals(ordinal)) {
            return 28;
        }else if (TransactionType.INCREASED_QUOTA.equals(ordinal)) {
            return 29;
        }else if (TransactionType.CTO_SIGN_IN_AWARD.equals(ordinal)) {
            return 30;
        }else if (TransactionType.TRANSFORMATION.equals(ordinal)) {
            return 31;
        }else if (TransactionType.LOWER_SHELF.equals(ordinal)) {
            return 32;
        }else if (TransactionType.UPPER_SHELF.equals(ordinal)) {
            return 33;
        }else if (TransactionType.COIN_RELEASE.equals(ordinal)) {
            return 34;
        }else if (TransactionType.CANCELLATION_OF_ORDER.equals(ordinal)) {
            return 35;
        }else if (TransactionType.BUYING_COINS.equals(ordinal)) {
            return 36;
        }else if (TransactionType.CTORECOMMENDATIONAWARD.equals(ordinal)) {
            return 37;
        }else if (TransactionType.RUSH_TO_PURCHASE.equals(ordinal)) {
            return 38;
        }else if (TransactionType.RUSH_TO_PURCHASE_DEDUCT.equals(ordinal)) {
            return 39;
        }else if (TransactionType.DIRECT_PUSH_PANIC_BUYING_AWARD.equals(ordinal)) {
            return 40;
        }else if (TransactionType.REWARDS_TO_RELEASE.equals(ordinal)) {
            return 41;
        }else if (TransactionType.FROZEN.equals(ordinal)) {
            return 42;
        }else if (TransactionType.UNFREEZE.equals(ordinal)) {
            return 43;
        }else if (TransactionType.Robot_end_retreat.equals(ordinal)) {
            return 44;
        }
        else {
            return  999;
        }
    }

}

package cn.ztuo.bitrade.constant;

import com.fasterxml.jackson.annotation.JsonValue;

import cn.ztuo.bitrade.core.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author GS
 * @date 2018年03月08日
 */
@AllArgsConstructor
@Getter
public enum ActivityRewardType implements BaseEnum {

    /**
     * 注册奖励
     */
    REGISTER("注册奖励"),
    /**
     * 交易奖励
     */
    TRANSACTION("交易奖励"),
    /**
    /**
     * 充值奖励
     */
    RECHARGE("充值奖励"),

    /**
     /**
     * 借币还币
     */
    BORROWINGANDRETURNING("借币还币"),

    /**
     /**
     * VIP1升级规则及奖励配置
     */
    VIPONECONF("VIP1配置"),

    /**
     /**
     * VIP2升级规则及奖励配置
     */
    VIPTWOCONF("VIP2配置"),

    /**
     /**
     * VIP3升级规则及奖励配置
     */
    VIPTHREECONF("VIP3配置");


    @Setter
    private String cnName;

    @Override
    @JsonValue
    public int getOrdinal() {
        return ordinal();
    }
    
}

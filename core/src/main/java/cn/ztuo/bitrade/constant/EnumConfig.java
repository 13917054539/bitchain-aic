package cn.ztuo.bitrade.constant;

/**
 * @author:
 * @Date: Created in 9:38 2018/11/22
 */
public enum EnumConfig {

    /**
     *
     */
    SALES_QUANTITY("sales_quantity", "销售calorie数量"),
    SNAP_UP_MAXIMUM("snap_up_maximum", "单人最大抢购数量（usdt）"),
    CURRENT_PERIOD_PRICE("current_period_price", "本期价格 1 calorie = ***usdt"),
    CURRENT_SURPLUS_QUANTITY("current_surplus_quantity", "本期剩余数量"),
    THE_NEXT_PRICE("the_next_price", "下期价格 1 calorie = ***usdt"),
    START_TIME("start_time", "抢购开始时间"),
    NO_LIMIT_ID("no_limit_id ", "不限制分享抢购奖励id(逗号隔开)"),
    ;


    /**
     * 规则代码
     */
    private String code;
    /**
     * 规则描述
     */
    private String description;

    EnumConfig(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

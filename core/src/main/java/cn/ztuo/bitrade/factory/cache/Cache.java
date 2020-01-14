package cn.ztuo.bitrade.factory.cache;

/**
 * 所有缓存名称的集合
 *
 */
public interface Cache {

    /**
     * 常量缓存
     */
    String CONSTANT = "CONSTANT";
    /**
     * 系统配置
     */
    String SYS_CONFIG = "SYS_CONFIG:";
    /**
     * 国家电话代码
     */
    String SMS_CODE = "SMS_CODE";

    /**
     * QIS 首页数据展示
     * */
    String QIS_LIST_KEY = "QIS_LIST_KEY:";

    /**
     * EIS 首页数据展示
     * */
    String EIS_LIST_KEY = "EIS_LIST_KEY:";

    /**
     * 升级奖励
     */
    String UPGRADE_AWARDS = "UPGRADE_AWARDS:";
    /**
     * 卖单限制
     */
    String SALE_ASTRICT = "SALE_ASTRICT:";


    /**
     * AIOS时价
     */
    String AIOS_RATE = "AIOS_RATE";
}

package cn.ztuo.bitrade.factory.cache;

/**
 * 缓存标识前缀集合,常用在ConstantFactory类中
 */
public interface CacheKey {

    /**
     * 角色名称(多个)
     */
    String ROLES_NAME = "'roles_name'";
    /**
     * 国家代码
     */
    String SMS_CODE_KEY = "'sms_code_key'";
    /**
     * 角色名称(单个)
     */
    String SINGLE_ROLE_NAME = "'single_role_name'";

    /**
     * 角色英文名称
     */
    String SINGLE_ROLE_TIP = "'single_role_tip'";

    /**
     * 部门名称
     */
    String DEPT_NAME = "'dept_name'";

    /**
     * 规则代码
     */
    String REGULATION_CODE = "'regulation_code_'";



}

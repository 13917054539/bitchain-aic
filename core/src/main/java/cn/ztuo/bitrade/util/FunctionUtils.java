package cn.ztuo.bitrade.util;

import java.math.BigDecimal;

/**
 * @Auther:路道
 * @Date:2019/6/14
 * @Description:com.qistoken.admin.core.util
 * @version:1.0
 */
public class FunctionUtils {

    /**
     * 加
     * @param a1 加数
     * @param a2 被加数
     * @param index 保留位数
     * @return 四舍五入 保留两位小数
     */
    public static BigDecimal add(BigDecimal a1, BigDecimal a2, int index){
        return a1.add(a2).setScale(index, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 减
     * @param a1 减数
     * @param a2 被减数
     * @param index 保留位数
     * @return 四舍五入 保留两位小数
     */
    public static BigDecimal sub(BigDecimal a1, BigDecimal a2,int index){
        return a1.subtract(a2).setScale(index, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 乘
     * @param a1 乘数
     * @param a2 被乘数
     * @param index 保留位数
     * @return 四舍五入 保留两位小数
     */
    public static BigDecimal mul(BigDecimal a1, BigDecimal a2,int index){
        return a1.multiply(a2).setScale(index, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 除
     * @param d1 除数
     * @param d2 被除数
     * @param index 保留位数
     * @return 四舍五入 保留两位小数
     */
    public static BigDecimal div(BigDecimal d1, BigDecimal d2,int index){
        return d1.divide(d2).setScale(index, BigDecimal.ROUND_HALF_UP);
    }
}

package cn.ztuo.bitrade.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;

/**
 * @Auther:路道
 * @Date:2019/6/26
 * @Description:cn.ztuo.bitrade.entity
 * @version:1.0
 */
@Data
@Entity
public class PatternConf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 30天内的提现手续费 根据第一次充值时间
     */
    private BigDecimal withinpaymentfee;

    /**
     * 30天以后的提现手续费 根据第一次充值时间
     */
    private BigDecimal afterpaymentfee;

    /**
     * 封顶层数
     */
    private Integer layernumber;

    /**
     * 推荐收益
     */
    private BigDecimal directrevenue;

    /**
     * 层级收益
     */
    private BigDecimal hierarchicalincome;

    /**
     * 直推有效会员人数可达到s1
     */
    private Integer effectivemembership;

    /**
     * S1团队业绩
     */
    private BigDecimal oneteamperformance;

    /**
     * S1奖励比例
     */
    private BigDecimal oneproportion;

    /**
     * S2团队业绩
     */
    private BigDecimal twoteamperformance;

    /**
     * 拥有几个S1
     */
    private Integer havingone;

    /**
     * S2奖励比例
     */
    private BigDecimal twoproportion;

    /**
     * S3团队业绩
     */
    private BigDecimal threeteamperformance;

    /**
     * 拥有几个S2
     */
    private Integer havingtwo;

    /**
     * S3奖励比例
     */
    private BigDecimal threeproportion;

    /**
     * 加权奖励
     */
    private BigDecimal weightedreward;

    /**
     * 释放比例
     */
    private BigDecimal releaseratio;


    /**
     * 币数量达到才可释放
     */
    private BigDecimal usdtnum;

    /**
     * USDT数量达到多少才算是有效会员
     */
    private BigDecimal usdtnummember;


    /**
     * 统计团队业绩层数
     */
    private Integer teamlayer;

    /**
     * 拥有几个S3可成为SP
     */
    private Integer havingthree;

    /**
     * 提币手续费
     */
    private BigDecimal withdrawalprotie;



}

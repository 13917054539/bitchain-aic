package cn.ztuo.bitrade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Data
@Entity
public class RobotOrder {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private BigInteger id;

    private Long memberId;

    //剩余金额
    private BigDecimal balance;

    //投入金额单位默认为U
    private BigDecimal amount;

    //此笔订单手续费
    private  BigDecimal free;

    //0为买 1为卖
    private int type;

    //状态 0为启动中 ,1为已关闭 ,2
    private int status;

    //买入或者卖出的比率
    private BigDecimal rate;

    //买入或者卖出的比率
    private BigDecimal aiosRate;

    //买入或卖出时价
    private  BigDecimal purchasePrice;

    //符合比率时买入或者卖出的数量
    private BigDecimal numberPurchases;

    //关闭时剩余金额
    private BigDecimal residueAmount;

    //下架时退回的时价
    private BigDecimal endPurchasePrice;

    //下架时退回的数量
    private BigDecimal endQuantity;

    //总收益
    private BigDecimal totalAmount;

    //机器人发送的订单id
    private String exchangeOrderId;

    //创建时间
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private BigDecimal sellRate;

    @Transient
    private BigDecimal buyRate;

    private int isRelease;
    private String remark;

    @Transient
    private Long sellRobotOrderId;



}

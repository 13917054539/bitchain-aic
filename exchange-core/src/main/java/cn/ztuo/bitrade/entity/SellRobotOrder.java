package cn.ztuo.bitrade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Data
@Entity
public class SellRobotOrder {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Long memberId;

    //投入金额单位默认为U
    private BigDecimal amount;

    //状态 0为启动中 ,1为已关闭 ,2
    private int status;

    //买入或者卖出的比率
    private BigDecimal rate;

    //创建时间
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

     private BigDecimal buyRate;

}

package cn.ztuo.bitrade.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.ztuo.bitrade.constant.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @desc 会员交易记录，包括充值、提现、转账等
 *
 */
@Entity
@Data
public class RobotTransaction {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Long memberId;
    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 交易类型
     */
    @Enumerated(EnumType.ORDINAL)
    private TransactionType type;
    /**
     * 币种名称，如 BTC
     */
    private String symbol;

    /**
     * 交易手续费
     * 提现和转账才有手续费，充值没有;如果是法币交易，只收发布广告的那一方的手续费
     */
    @Column(precision = 26,scale = 16)
    private BigDecimal fee = BigDecimal.ZERO ;

}

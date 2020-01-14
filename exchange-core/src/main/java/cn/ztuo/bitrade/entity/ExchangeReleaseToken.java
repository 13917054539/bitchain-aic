package cn.ztuo.bitrade.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Document(collection = "exchange_release_token")
public class ExchangeReleaseToken {

    private long memberId;
    private String orderId;
    /**
     * 释放后的剩余数量
     * */
    private BigDecimal balance;
    /**
     * 释放数量
     * */
    private BigDecimal releaseAmount;
    /**
     * 释放时间
     * */
    private long releaseTime;
}

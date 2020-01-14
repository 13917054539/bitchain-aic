package cn.ztuo.bitrade.entity;


import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.math.BigDecimal;

@Data
@Document(collection = "exchange_huobi_tickers")
public class HuoBiTickers {

    @Id
    private String symbol;
    private BigDecimal open = BigDecimal.ZERO;
    private BigDecimal close  = BigDecimal.ZERO;
    private BigDecimal low  = BigDecimal.ZERO;
    private BigDecimal high  = BigDecimal.ZERO;
    private BigDecimal amount  = BigDecimal.ZERO;
    private BigDecimal count  = BigDecimal.ZERO;
    private BigDecimal vol  = BigDecimal.ZERO;


}

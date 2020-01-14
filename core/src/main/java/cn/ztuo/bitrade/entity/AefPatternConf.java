package cn.ztuo.bitrade.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * @Auther:路道
 * @Date:2019/8/6
 * @Description:cn.ztuo.bitrade.entity
 * @version:1.0
 */
@Entity
@Data
@Table
public class AefPatternConf {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    private BigDecimal registrationaward;

    private Integer numbersatisfaction;

    private BigDecimal releaseratio;

    private BigDecimal minnum;

    private BigDecimal maxnum;

    private BigDecimal satisfyingassets;

    private BigDecimal onereward;

    private BigDecimal tworeward;

    private BigDecimal threereward;

    private BigDecimal fivereward;

    private BigDecimal fourreward;

    private BigDecimal sixreward;

    private BigDecimal signinaward;

    private BigDecimal ctoregistrationaward;

    private BigDecimal ctosigninaward;

    private BigDecimal ctocurrency;

    private BigDecimal ctoincrease;

    private BigDecimal ctorecommendationaward;

}

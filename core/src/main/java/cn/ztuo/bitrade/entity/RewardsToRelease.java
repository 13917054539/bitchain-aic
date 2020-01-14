package cn.ztuo.bitrade.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 奖励释放表
 *
 * @author
 * @date
 */
@Entity
@Data
@Table(name = "rewards_to_release")
public class RewardsToRelease {
    @Excel(name = "主键", orderNum = "1", width = 20)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    /**
     * 用户id
     */
    @Excel(name = "用户id", orderNum = "1", width = 20)
    @Column(unique = true, nullable = false)
    private Long memberId;

    /**
     * 总数量
     */
    @Excel(name = "总数量", orderNum = "1", width = 20)
    @Column(unique = true, nullable = false)
    private BigDecimal totalQuantity;

    /**
     * 释放天数
     */
    @Excel(name = "释放天数", orderNum = "1", width = 20)
    @Column(unique = true, nullable = false)
    private Integer releaseDays;


    /**
     * 已释放数量
     */
    @Excel(name = "已释放数量", orderNum = "1", width = 20)
    @Column(unique = true, nullable = false)
    private BigDecimal releasedQuantity;

    /**
     * 待释放数量
     */
    @Excel(name = "待释放数量", orderNum = "1", width = 20)
    @Column(unique = true, nullable = false)
    private BigDecimal quantityToReleased;


    /**
     * 标识 如：CAL
     */
    @Excel(name = "标识 如：CAL", orderNum = "1", width = 20)
    @Column(unique = true, nullable = false)
    private String syboml;

    /**
     * 奖励来源
     */
    @Excel(name = "奖励来源", orderNum = "1", width = 20)
    @Column(unique = true, nullable = false)
    private String remarks;

    /**
     * 创建时间
     */
    @Excel(name = "创建时间", orderNum = "1", width = 20)
    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

}

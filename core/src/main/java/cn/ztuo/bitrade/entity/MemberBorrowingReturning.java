package cn.ztuo.bitrade.entity;

/**
 * @Auther:路道
 * @Date:2019/6/18
 * @Description:cn.ztuo.bitrade.entity
 * @version:1.0
 */

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 借币买币记录
 *
 * @author GS
 * @date 2018年01月02日
 */
@Entity
@Data
public class MemberBorrowingReturning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Long mid;

    private String paycode;

    private BigDecimal wbcnum;

    private Integer type;

    private Integer status;

    private Date createtime;

    private Date returntime;


}



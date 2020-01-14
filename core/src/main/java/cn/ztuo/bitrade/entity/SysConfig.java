package cn.ztuo.bitrade.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * qis 系统规则配置表
 * @author zhang yingxin
 * @date 2018/5/5
 */
@Entity
@Data
@Table(name = "sys_config")
public class SysConfig {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Excel(name = "类型", orderNum = "1", width = 25)
    @Column(nullable = false,unique = true)
    private Integer type;

    @Excel(name = "类型名称", orderNum = "1", width = 25)
    @Column(nullable = false,unique = true)
    private String typeName;

    @Excel(name = "规则名称", orderNum = "1", width = 25)
    @Column(nullable = false,unique = true)
    private String cnName;


    @Excel(name = "规则取值", orderNum = "1", width = 25)
    //@NotBlank(message = "规则取值不能为空")
    @Column(nullable = false,unique = true)
    private String reValue;

    @Excel(name = "是否删除", orderNum = "1", width = 25)
    @Column(nullable = false,unique = true)
    private Integer isDel;

    @Excel(name = "备注", orderNum = "1", width = 25)
    @Column(nullable = false,unique = true)
    private String remark;

    @Excel(name = "创建时间", orderNum = "1", width = 25)
    @Column(nullable = false,unique = true)
    private Date createTime;


}

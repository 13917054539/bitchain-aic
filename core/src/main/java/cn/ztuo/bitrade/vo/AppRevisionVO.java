package cn.ztuo.bitrade.vo;

import cn.ztuo.bitrade.constant.Platform;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * @author GS
 * @date 2018年04月24日
 */
@Data
public class AppRevisionVO {

    private Long id;

    private Date publishTime;

    private String remark;

    private String version;

    private String downloadUrl;

    private Platform platform;

    private List<String> stringList;

}

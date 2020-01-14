package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.AefPatternConf;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @Auther:路道
 * @Date:2019/8/6
 * @Description:cn.ztuo.bitrade.dao
 * @version:1.0
 */
public interface AefPatternConfDao  extends BaseDao<AefPatternConf> {

    @Query(value = "SELECT * FROM aef_pattern_conf WHERE id=:id", nativeQuery = true)
    AefPatternConf getAefPatternConf(@Param("id")Integer id);
}

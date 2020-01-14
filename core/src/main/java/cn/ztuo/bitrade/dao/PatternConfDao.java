package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.BusinessAuthApply;
import cn.ztuo.bitrade.entity.PatternConf;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

/**
 * @Auther:路道
 * @Date:2019/6/26
 * @Description:cn.ztuo.bitrade.dao
 * @version:1.0
 */
public interface PatternConfDao extends BaseDao<PatternConf> {

    /**
     * 功能描述:获取模式配置信息
     * @param:
     * @return:
     */
    @Query(value ="SELECT * FROM  pattern_conf WHERE id=1",nativeQuery = true)
    PatternConf getPatternConf();

}

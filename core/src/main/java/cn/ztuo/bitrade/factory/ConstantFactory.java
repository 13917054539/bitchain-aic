package cn.ztuo.bitrade.factory;

import cn.ztuo.bitrade.dao.SysConfigDao;
import cn.ztuo.bitrade.entity.SysConfig;
import cn.ztuo.bitrade.factory.cache.Cache;
import cn.ztuo.bitrade.factory.cache.CacheKey;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 常量的生产工厂
 */
@Component
public class ConstantFactory {

    @Resource
    private SysConfigDao sysConfigDao;

    /**
     * 获取规则的值
     */
//    @Cacheable(value = Cache.SYS_CONFIG, key =  CacheKey.REGULATION_CODE + "+#id", unless="#result==null")
    public String getRegulationValueById(Long id) {
        SysConfig regulation = sysConfigDao.findOne(id);
        return regulation.getReValue();
    }
}

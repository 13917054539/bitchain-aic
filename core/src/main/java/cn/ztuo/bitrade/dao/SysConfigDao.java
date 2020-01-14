package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

@Component
public interface SysConfigDao extends JpaRepository<SysConfig,String>, JpaSpecificationExecutor<SysConfig>, QueryDslPredicateExecutor<SysConfig> {

    @Query(value = "SELECT * FROM sys_config where id =:id",nativeQuery = true)
    SysConfig findOne(@Param("id") Long id);
}

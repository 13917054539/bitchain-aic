package cn.ztuo.bitrade.dao;


import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.RobotOrder;
import cn.ztuo.bitrade.entity.SellRobotOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SellRobotOrderDao extends BaseDao<SellRobotOrder> {


    @Query(value = "SELECT\n" +
            "\t* \n" +
            "FROM\n" +
            "\tsell_robot_order \n" +
            "WHERE\n" +
            "\t`status` = 0 \n" +
            "\tAND DATE_ADD(create_time, INTERVAL (SELECT re_value FROM sys_config WHERE id = 1) minute ) <= now()" +
            "ORDER BY create_time", nativeQuery = true)
    List<SellRobotOrder> getSellRobot();

    @Query(value = "SELECT\n" +
            "\t* \n" +
            "FROM\n" +
            "\tsell_robot_order \n" +
            "WHERE\n" +
            "\t id = :id  ", nativeQuery = true)
    SellRobotOrder getOrderById(@Param("id") Long id);

    List<SellRobotOrder> findByMemberIdAndStatus(Long memberId ,Integer status);
}

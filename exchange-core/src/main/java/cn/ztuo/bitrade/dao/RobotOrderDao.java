package cn.ztuo.bitrade.dao;


import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.RobotOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RobotOrderDao extends BaseDao<RobotOrder> {
    @Query(value = "select * from robot_order where member_id=:memberId and `status`=:status ", nativeQuery = true)
    List<RobotOrder> getAllByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") Integer status);

    RobotOrder findByExchangeOrderId(String exchangeOrderId);

    @Query(value = "SELECT\n" +
            "\t* \n" +
            "FROM\n" +
            "\trobot_order \n" +
            "WHERE\n" +
            "\t`status` = 0 \n" +
            "\tAND DATE_ADD( create_time, INTERVAL 3 HOUR ) <= now()", nativeQuery = true)
    List<RobotOrder> getRobotByStatusAnAndCreateTime();

    @Query(value = "SELECT\n" +
            "\t* \n" +
            "FROM\n" +
            "\trobot_order \n" +
            "WHERE\n" +
            "\t`status` = 0 \n", nativeQuery = true)
    List<RobotOrder> getRobotByStatus();

    @Query(value = "SELECT\n" +
            "\t* \n" +
            "FROM\n" +
            "\trobot_order \n" +
            "WHERE\n" +
            "\t`status` = 1 AND is_release= 0 and type =0 \n", nativeQuery = true)
    List<RobotOrder> getReleaseOrder();

    @Query(value = "SELECT\n" +
            "\t IFNULL( SUM(balance),0.000)  \n" +
            "FROM\n" +
            "\trobot_order \n" +
            "WHERE\n" +
            "\t`status` = 1  AND type =0 AND member_id =:memberId " +
            "AND DATE_FORMAT(create_time,'%Y-%m-%d')=DATE_FORMAT(CURDATE() ,'%Y-%m-%d') \n", nativeQuery = true)
    BigDecimal getTodayAmount(@Param("memberId") Long memberId);

    @Query(value = "SELECT\n" +
            "\t IFNULL( SUM(balance) ,0.000)  \n" +
            "FROM\n" +
            "\trobot_order \n" +
            "WHERE\n" +
            "\t `status` = 1  AND type =0 AND DATE_FORMAT(create_time,'%Y-%m-%d')=DATE_FORMAT(CURDATE() ,'%Y-%m-%d')", nativeQuery = true)
    BigDecimal getTodayAmountSum();

    @Query(value = "SELECT\n" +
            "\t  IFNULL(\tROUND( SUM( IF ( balance IS NULL, 0, balance )* IF ( aios_rate IS NULL, 0, aios_rate )), 5 ) ,0)  \n" +
            "FROM\n" +
            "\t robot_order \n" +
            "WHERE\n" +
            "\t`status` = 0  AND type =0 AND member_id =:memberId \n", nativeQuery = true)
    BigDecimal getByAmountSum(@Param("memberId") Long memberId);

    @Query(value = "\tSELECT\n" +
            "IFNULL( SUM( balance ), 0.000 ) \n" +
            "FROM\n" +
            "\trobot_order \n" +
            "WHERE\n" +
            "\t`status` = 1 \n" +
            "\tAND type = :type  \n" +
            "\tAND DATE_FORMAT( create_time, '%Y-%m-%d' )= DATE_FORMAT(\n" +
            "\tCURDATE(),\n" +
            "\t'%Y-%m-%d')", nativeQuery = true)
    BigDecimal getOrderSumToday(@Param("type") Integer type);

    @Query(value = "\tSELECT\n" +
            "IFNULL( SUM( balance ), 0.000 ) \n" +
            "FROM\n" +
            "\trobot_order ", nativeQuery = true)
    BigDecimal getHistoricalGrossTransactionVolume();

    @Query(value = "\tSELECT\n" +
            "IFNULL( SUM( balance ), 0.000 ) \n" +
            "FROM\n" +
            "\trobot_order \n" +
            "WHERE   DATE_FORMAT( create_time, '%Y-%m-%d' )= DATE_FORMAT(\n" +
            "\tCURDATE(),\n" +
            "\t'%Y-%m-%d')", nativeQuery = true)
    BigDecimal getTotalTurnoverToday();





}

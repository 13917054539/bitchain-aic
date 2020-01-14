package cn.ztuo.bitrade.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.MemberTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Date;

public interface MemberTransactionDao extends BaseDao<MemberTransaction> {

    @Query("select m from MemberTransaction as m where m.createTime between :startTime and  :endTime and m.type = :type")
    List<MemberTransaction> findAllDailyMatch(String startTime,String endTime,TransactionType type);

    /*@Query(value="select sum(amount),sum(fee) ,symbol as unit from member_transaction where type = 3 and date_format(create_time,'%Y-%m-%d')= :date group by symbol",nativeQuery = true)
    List<Object[]> getExchangeTurnover(@Param("date") String date);*/

    @Query("select sum(t.amount)  as amount from MemberTransaction t where t.flag = 0 and t.amount > 0 and t.memberId = :memberId and t.symbol = :symbol and t.type in :types")
    Map<String,Object> findMatchTransactionSum(@Param("memberId") Long memberId,@Param("symbol") String symbol,@Param("types") List<TransactionType> types);

    @Query("select sum(t.amount)  as amount from MemberTransaction t where t.flag = 0  and t.memberId = :memberId and t.symbol = :symbol and t.type = :type and t.createTime >= :startTime and t.createTime <= :endTime")
    Map<String,Object> findMatchTransactionSum(@Param("memberId") Long memberId,@Param("symbol") String symbol,@Param("type") TransactionType type,@Param("startTime") Date startTime,@Param("endTime") Date endTime);

    @Query("select sum(t.amount)  as amount from MemberTransaction t where t.flag = 0  and t.symbol = :symbol and t.type = :type and t.createTime >= :startTime and t.createTime <= :endTime")
    Map<String,Object> findMatchTransactionSum(@Param("symbol") String symbol,@Param("type") TransactionType type,@Param("startTime") Date startTime,@Param("endTime") Date endTime);

    @Query(value = "SELECT IFNULL(SUM(amount),0) FROM member_transaction WHERE type=0 AND member_id in(:longId) ",nativeQuery = true)
    BigDecimal teamAchievement(@Param("longId")List<Long> longId);

    /**
     * 功能描述:查看今日释放总量
     * @param:
     * @return:
     */
    @Query(value = "SELECT IFNULL(SUM(amount),0) FROM member_transaction WHERE type=18 AND to_days(create_time) = to_days(now())",nativeQuery = true)
    BigDecimal totalRelease();

    /**
     * 功能描述: 最新一次加入合约时间
     * @param:
     * @return:
     */
    @Query(value = "SELECT * FROM member_transaction WHERE member_id=:id AND type=19 AND symbol=:symbol  ORDER BY id DESC LIMIT 1",nativeQuery = true)
    MemberTransaction getPaymentFee(@Param("id") long id,@Param("symbol")String symbol);

    /**
     * 功能描述: 统计今日流水
     * @param:
     * @return:
     */
    @Query(value = "SELECT IFNULL(SUM(amount),0) FROM member_transaction WHERE type=:type AND member_id =:memberId AND to_days(create_time) = to_days(now()) AND amount>0",nativeQuery = true)
    BigDecimal toDayamountNum(@Param("memberId") Long memberId,@Param("type") Integer type);

    /**
     *
     * 功能描述:统计奖励
     * @param memberId
     * @param type
     * @return
     */
    @Query(value = "select abs(IFNULL(SUM(iFNULL(amount,0)),0)) from member_transaction where type=:Istype and member_id=:memberId ",nativeQuery = true)
    BigDecimal getSumReward(@Param("memberId") long memberId,@Param("Istype") int type);


    /**
     *
     * 功能描述:查询用户已抢购CAL数量
     * @param memberId
     * @param type
     * @return
     */
    @Query(value = "select abs(IFNULL(SUM(amount),0)) from member_transaction where type=:type and member_id=:memberId ",nativeQuery = true)
    BigDecimal getquantityPurchased(@Param("memberId") long memberId,@Param("type") int type);
}

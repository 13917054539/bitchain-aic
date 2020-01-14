package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.Admin;
import cn.ztuo.bitrade.entity.Department;
import cn.ztuo.bitrade.entity.RewardsToRelease;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author GS
 * @date 2017年12月18日
 */
public interface RewardsToReleaseDao extends BaseDao<RewardsToRelease> {
    /**
     * 用户所有已释放奖励
     * @param memberId
     * @return
     */
    @Query(value = "select ifnull(sum(released_quantity),0) from rewards_to_release where member_id = :memberId",nativeQuery = true)
    BigDecimal sumReleasedQuantity(@Param("memberId") Long memberId);
    /**
     * 用户所有待释放奖励
     * @param memberId
     * @return
     */
    @Query(value = "select ifnull(sum(quantity_to_released),0) from rewards_to_release where member_id = :memberId",nativeQuery = true)
    BigDecimal sumQuantityToReleased(@Param("memberId") Long memberId);

    /**
     * 获取全部未释放完的奖励记录
     * @return
     */
    @Query(value = "select * from rewards_to_release where quantity_to_released > 0",nativeQuery = true)
    List<RewardsToRelease> findAllByQuantityToReleasedGt();

    List<RewardsToRelease> findAllByMemberId(Long MemberId, Pageable pageable);
}

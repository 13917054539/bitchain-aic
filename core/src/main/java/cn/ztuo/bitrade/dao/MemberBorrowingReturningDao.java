package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.MemberBorrowingReturning;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Auther:路道
 * @Date:2019/6/18
 * @Description:cn.ztuo.bitrade.dao
 * @version:1.0
 */
public interface MemberBorrowingReturningDao extends BaseDao<MemberBorrowingReturning> {

    /**
     * 功能描述: 根据用户id查询出会员是否有未归还的借款信息
     * @param:
     * @return:
     */
    @Query(value = "SELECT COUNT(1) FROM member_borrowing_returning WHERE mid=:id AND status=0", nativeQuery = true)
    int borrowingRecordDetile(@Param("id") Long id);

    /**
     * 功能描述: 查看用户的欠款信息
     * @param:
     * @return:
     */
    @Query(value = "SELECT * FROM member_borrowing_returning WHERE mid= :id AND status=0", nativeQuery = true)
    MemberBorrowingReturning InformationOnArrears(@Param("id") Long id);

}

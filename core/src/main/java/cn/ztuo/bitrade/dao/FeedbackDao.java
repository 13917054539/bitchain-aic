package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.Feedback;
import cn.ztuo.bitrade.entity.Member;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @author GS
 * @date 2018年03月19日
 */
public interface FeedbackDao extends BaseDao<Feedback> {

    List<Feedback> findByMember_Id(Long id);

    List<Feedback> findByMemberOrderByCreateTimeDesc(Member member, Pageable pageable);


    List<Feedback> findAllByMember_Id(Long id);


}

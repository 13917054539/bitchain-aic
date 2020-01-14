package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.MemberApplication;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cn.ztuo.bitrade.dao.FeedbackDao;
import cn.ztuo.bitrade.entity.Feedback;
import cn.ztuo.bitrade.service.Base.BaseService;

import java.util.List;

/**
 * @author GS
 * @date 2018年03月19日
 */
@Service
public class FeedbackService extends BaseService {

    @Autowired
    private FeedbackDao feedbackDao;

    public  Feedback  findById(Long mid){
        return feedbackDao.findOne(mid);
    }
    public List<Feedback> findByMember(Member member, Pageable pageable){
        return feedbackDao.findByMemberOrderByCreateTimeDesc(member,pageable);
    }
    public  List<Feedback>  findAllByMember_Id(Long mid){
        return feedbackDao.findAllByMember_Id(mid);
    }

    public Page<Feedback> findAll(Predicate predicate, Pageable pageable) {
        return feedbackDao.findAll(predicate, pageable);
    }

    public Feedback save(Feedback feedback){
        return feedbackDao.save(feedback);
    }
    public void delete(Long uid){
        feedbackDao.delete(uid);
    }



}

package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.constant.AuditStatus;
import cn.ztuo.bitrade.constant.PageModel;
import cn.ztuo.bitrade.entity.QFeedback;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.extern.slf4j.Slf4j;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import cn.ztuo.bitrade.entity.Feedback;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.service.FeedbackService;
import cn.ztuo.bitrade.service.LocaleMessageSourceService;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.util.MessageResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author GS
 * @date 2018年03月19日
 */
@RestController
@Slf4j
public class FeedbackController extends BaseController{
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private LocaleMessageSourceService msService;

    /**
     * 提交反馈意见
     *
     * @param user
     * @param remark
     * @return
     */
    @RequestMapping("feedback")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult feedback(@SessionAttribute(SESSION_MEMBER) AuthMember user, String remark,String imageUrl,String contact) {
        Feedback feedback = new Feedback();
        Member member = memberService.findOne(user.getId());
        feedback.setMember(member);
        feedback.setRemark(remark);
        feedback.setStatus(AuditStatus.AUDIT_ING);
        feedback.setPictureUrl(imageUrl);
        feedback.setContact(contact);
        Feedback feedback1 = feedbackService.save(feedback);
        if (feedback1 != null) {
            return MessageResult.success();
        } else {
            return MessageResult.error(msService.getMessage("SYSTEM_ERROR"));
        }
    }




    /**
     * 反馈意见列表
     * @param user
     * @return
     */
    @RequestMapping("feedbackList")
    public MessageResult feedbacklist(
            @SessionAttribute(SESSION_MEMBER) AuthMember user,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "30") Integer pageSize) {

        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = new PageRequest(pageNo-1, pageSize, sort);
        List<Feedback> page = feedbackService.findByMember(memberService.findOne(user.getId()),pageable);
        return success(page);
    }



}

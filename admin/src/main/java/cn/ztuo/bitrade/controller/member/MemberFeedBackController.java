package cn.ztuo.bitrade.controller.member;

import cn.ztuo.bitrade.annotation.AccessLog;
import cn.ztuo.bitrade.constant.AdminModule;
import cn.ztuo.bitrade.constant.AuditStatus;
import cn.ztuo.bitrade.constant.PageModel;
import cn.ztuo.bitrade.controller.common.BaseAdminController;
import cn.ztuo.bitrade.entity.Feedback;
import cn.ztuo.bitrade.entity.QFeedback;
import cn.ztuo.bitrade.entity.QRewardActivitySetting;
import cn.ztuo.bitrade.service.FeedbackService;
import cn.ztuo.bitrade.util.MessageResult;
import com.google.common.collect.Maps;
import com.mashape.unirest.http.utils.MapUtil;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @author GS
 * @description 会员投诉建议
 * @date 2017/12/27 10:16
 */
@RestController
@Slf4j
@RequestMapping("feedback")
public class MemberFeedBackController extends BaseAdminController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("page")
    @AccessLog(module = AdminModule.MEMBER, operation = "所有反馈记录")
    public MessageResult pageQuery(
            PageModel pageModel,
            @RequestParam(value = "status", defaultValue = "") AuditStatus auditStatus) {
        //List<Feedback> all = feedbackService.findAllByMember_Id(mid);
        BooleanExpression predicate = null;
        if(auditStatus!=null){
            predicate = QFeedback.feedback.status.eq(auditStatus);
        }
        Page<Feedback> all = feedbackService.findAll(predicate,pageModel.getPageable());
        return success(all);
    }

    @PostMapping("audit")
    @AccessLog(module = AdminModule.MEMBER, operation = "审核反馈")
    public MessageResult update(
            @RequestParam(value = "id") Long id,
            @RequestParam(value = "opinion", defaultValue = "") String opinion,
            @RequestParam(value = "status", defaultValue = "") AuditStatus auditStatus) throws Exception {
        Feedback feedback = feedbackService.findById(id);
        if(feedback!=null){
            feedback.setOpinion(opinion);
            feedback.setStatus(auditStatus);
            feedbackService.save(feedback);
        }
        return success();
    }


    @PostMapping("delete")
    @AccessLog(module = AdminModule.MEMBER, operation = "审核反馈")
    public MessageResult delete(
            @RequestParam(value = "id") Long id) throws Exception {
        feedbackService.delete(id);
        return success();
    }

}

package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.service.BorrowingAndReturningService;
import cn.ztuo.bitrade.service.LocaleMessageSourceService;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.util.Md5;
import cn.ztuo.bitrade.util.MessageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

/**
 * @Auther:路道
 * @Date:2019/6/18
 * @Description:cn.ztuo.bitrade.controller
 * @version:1.0
 */
@RestController
@RequestMapping("borrowingAndReturning")
public class BorrowingAndReturningController {

    @Autowired
    private BorrowingAndReturningService borrowingAndReturningService;

    @Autowired
    private LocaleMessageSourceService sourceService;

    @Autowired
    private MemberService memberService;

   /* @PostMapping("currency")
    public MessageResult borrowingAndReturning(@SessionAttribute(SESSION_MEMBER) AuthMember user,Integer type,String currencyNum,String pwd){
        MessageResult result=borrowingAndReturningService.borrowingAndReturning(user.getId(),type,currencyNum,pwd);
        return result;
    }*/


   /* @PostMapping("conf")
    public MessageResult borrowingAndReturningConf(@SessionAttribute(SESSION_MEMBER) AuthMember user){
        MessageResult result=borrowingAndReturningService.borrowingAndReturningConf(user.getId());
        return result;
    }*/
}

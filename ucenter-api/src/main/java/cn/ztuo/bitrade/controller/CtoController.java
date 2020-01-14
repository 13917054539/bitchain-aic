package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.service.CtoService;
import cn.ztuo.bitrade.util.MessageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

/**
 * @Auther:路道
 * @Date:2019/8/30
 * @Description:cn.ztuo.bitrade.controller
 * @version:1.0
 */
@Controller
@RestController
public class CtoController {

    @Autowired
    private CtoService ctoService;

    /** 
     * 功能描述: 进入CTO界面
     * @param: 
     * @return:
     */
    @RequestMapping("cto/showCto")
    public MessageResult showCto(@SessionAttribute(SESSION_MEMBER) AuthMember user){
      return  ctoService.showCto(user.getId());
    }

    /**
     * 功能描述: 签到CTO
     * @param:
     * @return:
     */
    @RequestMapping("cto/sign/in")
    public MessageResult ctoSignIn(@SessionAttribute(SESSION_MEMBER) AuthMember user){
        return ctoService.ctoSignIn(user.getId());
    }

    /**
     * 功能描述: 查看用户今日是否已经签到
     * @param:
     * @return:
     */
    @RequestMapping("cto/isCtoSignIn")
    public MessageResult isCtoSignIn(@SessionAttribute(SESSION_MEMBER) AuthMember user){
        return ctoService.isCtoSignIn(user.getId());
    }

    /**
     * 功能描述: CMB转CTO
     * @param:
     * @return:
     */
    @RequestMapping("cto/transformation")
    public MessageResult transformation(@SessionAttribute(SESSION_MEMBER) AuthMember user,String num){
        return ctoService.transformation(user.getId(),num);
    }
}

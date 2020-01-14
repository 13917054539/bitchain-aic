package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.service.AefPatternService;
import cn.ztuo.bitrade.service.ContractService;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

/**
 * @Auther:路道
 * @Date:2019/7/6
 * @Description:cn.ztuo.bitrade.controller
 * @version:1.0
 */
@RestController
@RequestMapping("contract")
public class ContractController {

    @Autowired
    private ContractService contractService;
    @Autowired
    private AefPatternService aefPatternService;

    /**
     * 功能描述:
     * @param: num 数量
     * @param: type 1加入 2解除
     * @param: coin 币种
     * @return:
     */
    @PostMapping("addOrRelieve")
    public MessageResult addOrRelieve(@SessionAttribute(SESSION_MEMBER) AuthMember user,String coin,String num,Integer type){
       return contractService.addOrRelieve(user.getId(),coin,num,type);
    }

    /**
     * 功能描述: 签到
     * @param:
     * @return:
     */
    @RequestMapping("sign/in")
    public MessageResult signIn(@SessionAttribute(SESSION_MEMBER) AuthMember user){
        return aefPatternService.signIn(user.getId());
    }


    /**
     * 功能描述: 查看用户是否已经签到
     * @param:
     * @return:
     */
    @RequestMapping("is/signIn")
    public MessageResult isSignIn(@SessionAttribute(SESSION_MEMBER) AuthMember user){
        return aefPatternService.isSignIn(user.getId());
    }

    /**
     * 功能描述: 查看用户资产信息
     * @param:
     * @return:
     */
    @RequestMapping("showAcount")
    public MessageResult showAcount(@SessionAttribute(SESSION_MEMBER) AuthMember user){
        return aefPatternService.showAcount(user.getId());
    }

    /**
     * 功能描述: 转换
     * @param:
     * @return:
     */
    @RequestMapping("transformation")
    public MessageResult transformation(@SessionAttribute(SESSION_MEMBER) AuthMember user){
        return aefPatternService.transformation(user.getId());
    }
}

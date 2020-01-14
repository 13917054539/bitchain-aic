package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.dao.SellRobotOrderDao;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.factory.ConstantFactory;
import cn.ztuo.bitrade.service.LocaleMessageSourceService;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.service.RobotOrderService;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

@Slf4j
@RestController
@RequestMapping("/robot")
public class RobotController extends MessageResult {
    @Autowired
    private MemberService memberService;
    @Autowired
    private LocaleMessageSourceService msService;
    @Autowired
    private RobotOrderService robotOrderService;


    @Autowired
    private ConstantFactory constantFactory;

    /**
     * 添加委托订单
     *
     * @param authMember
     * @param rate
     * @param amount
     * @param type
     * @return
     */
//    @RequestMapping("add")屏蔽客户端发布买单接口
    public MessageResult addOrder(@SessionAttribute(SESSION_MEMBER) AuthMember authMember, BigDecimal rate, BigDecimal sellRate, BigDecimal amount, String type) {
        RobotOrder robotOrder = new RobotOrder();
        if (rate != null && amount != null && type != null) {
            if (sellRate.compareTo(BigDecimal.ZERO) <= 0) {
                return error("卖出比率不允许小于或等于零");
            }
            if (sellRate.compareTo(new BigDecimal("1")) > 0) {
                return error("卖卖区间为0-1");
            }
            robotOrder.setMemberId(authMember.getId());
            robotOrder.setAmount(amount);
            robotOrder.setType(0);
            robotOrder.setSellRate(sellRate.divide(new BigDecimal("100"), 5, BigDecimal.ROUND_DOWN));
            robotOrder.setRate(rate.divide(new BigDecimal("100"), 5, BigDecimal.ROUND_DOWN));
            return robotOrderService.addRobotOrder(robotOrder);
        }
        return error("参数错误");

    }



    /**
     * 开启机器人
     * 开启委托
     *
     * @param authMember
     * @param amount 交易额
     * @return
     */
    @RequestMapping("openOperation")
    @Async
    public MessageResult openOperation(@SessionAttribute(SESSION_MEMBER) AuthMember authMember, BigDecimal amount, Integer status) {
        RobotOrder robotOrder = new RobotOrder();
        if ( status != null) {
            //Member会员用户
            Member member = memberService.findOne(authMember.getId());
            //开启
            if (status == 0) {
                //机器人关闭时间
                if(member.getRobotCloseTime()!=null){
                    Calendar cal =Calendar.getInstance();
                    cal.setTime(member.getRobotCloseTime());
                    cal.add(Calendar.MINUTE,30);
                    //当前时间与机器人关闭时间是否相差30分钟,之内则不能开启
                    if(cal.getTime().compareTo(new Date())>0){
                        return error("AI机器人关闭后不允许在30分钟内开启");
                    }
                }
                if(amount == null){
                    return error("请输入数量");
                    //AiosLimit:当前用户可以委托的数量
                }else if(amount.compareTo(member.getAiosLimit())>0){
                    return error("超过最大委托量");
                }
                if(member.getRobotStatus()==1){
                    return error("请勿频繁操作");
                }
                //根据委托人的会员id查询所有的委托订单
                List list=robotOrderService.getRobotOrderByMemberId(robotOrder.getMemberId());
                if (list.size()!=0) {return error("上一次机器人中断需要等待上一笔订单完成后才能进行下一笔订单");}
                //开启机器人
                robotOrder.setMemberId(authMember.getId());
                robotOrder.setAmount(amount);//投入金额单位默认为U
                robotOrder.setType(0);//0为买 1为卖
                robotOrder.setSellRate(new BigDecimal(0.0041));
                robotOrder.setRate(BigDecimal.ZERO);//买入或者卖出的比率
                //存入sellRobotOrder表中

                //下一步
                MessageResult messageResult= robotOrderService.addSellOrder(robotOrder);
                if(messageResult.getCode()==0){
                        member.setRobotStatus(1);
                        member.setRobotLimit(amount);
                        member.setRobotOpenTime(new Date());
                        memberService.save(member);
                    return messageResult;
                }else{
                    return messageResult;
                }
            } else {
//                return error("AI机器人暂不开放关闭");
                if(member.getRobotStatus()==0){
                    return error("请勿频繁操作");
                }
                if(member.getRobotOpenTime()!=null){
                    Calendar cal =Calendar.getInstance();
                    cal.setTime(member.getRobotOpenTime());
                    cal.add(Calendar.MINUTE,30);
                    if(cal.getTime().compareTo(new Date())>0){
                        return error("AI机器人关闭后不允许在30分钟内开启");
                    }
                }
                //关闭机器人
                List<RobotOrder> robotOrders= robotOrderService.getRobotOrderByMemberId(authMember.getId());
                member.setRobotStatus(0);
                member.setRobotLimit(BigDecimal.ZERO);
                member.setRobotCloseTime(new Date());
                memberService.save(member);
                robotOrderService.closeSellRobotOrder(member.getId());
                robotOrders.forEach(robotOrder1 ->{
                   robotOrderService.cancelOrder(member.getId(),robotOrder1.getExchangeOrderId());
                });
                return success("关闭委托系统会结束智能AI运行，未交易完成合约会强行平仓，可能造成损失。");
            }
        }
        return error("参数错误");

    }

    /**
     * 查看委托订单
     *
     * @param authMember
     * @param status
     * @return
     */
    @PostMapping("commissionedByHistory")
    public MessageResult CommissionedByHistory(@SessionAttribute(SESSION_MEMBER) AuthMember authMember, Integer status, @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo, @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return getSuccessInstance("获取成功", robotOrderService.CommissionedByHistory(authMember.getId(), status, pageNo, pageSize));
    }
}

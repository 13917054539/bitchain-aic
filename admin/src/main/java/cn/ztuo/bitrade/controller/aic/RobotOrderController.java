package cn.ztuo.bitrade.controller.aic;

import cn.ztuo.bitrade.dao.SysConfigDao;
import cn.ztuo.bitrade.entity.ExchangeOrderDirection;
import cn.ztuo.bitrade.service.RobotOrderService;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@Slf4j
@RequestMapping("robot")
public class RobotOrderController {
    @Autowired
    private RobotOrderService robotOrderService;


    /**
     * 获取历史总交易额,今日交易额,今日买单量,今日卖单量,以及机器人挂单记录
     */
    @PostMapping("getRobotOrderPage")
    public MessageResult getRobotOrderPage(Long memberId, Integer type, Integer pageNo, Integer pageSize) {
        return robotOrderService.getRobotOrderPage(memberId, type, pageNo, pageSize);
    }

    /**
     * 发布委托单
     */
    @PostMapping("addOrder")
    public MessageResult addOrder(BigDecimal price, BigDecimal amount, ExchangeOrderDirection exchangeOrderDirection) {
        return robotOrderService.addOrder(price, amount, exchangeOrderDirection);
    }

    /**
     * 查询机器人卖单延长时间
     */
    @PostMapping("getRobotTime")
    public MessageResult getRobotTime() {
        return robotOrderService.getRobotTime();
    }

    @PostMapping("getBAmount")
    public MessageResult getBAmount() {
        return robotOrderService.getBAmount();
    }

    /**
     * 修改机器人发布卖单延长时间
     */
    @PostMapping("updateRobotTime")
    public MessageResult updateRobotTime(String value) {
        return robotOrderService.updateRobotTime(value);
    }

    @PostMapping("updateBAmount")
    public MessageResult updateBAmount(String value) {
        return robotOrderService.updateBAmount(value);
    }
}

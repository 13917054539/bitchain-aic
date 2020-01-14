package cn.ztuo.bitrade;

import cn.ztuo.bitrade.JobApplication;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.service.*;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author GS
 * @date 2018年02月06日
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = JobApplication.class)
public class BasicApplicationTest {

    @Autowired
    private RobotOrderService robotOrderService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ExchangeOrderService exchangeOrderService;

    /**
     * 每分钟查询币币交易订单更新机器人状态 * 0-59 * * * ?
     */
    @Test
    public synchronized void scanComspletedOrder() {
        List<RobotOrder> robotOrders = robotOrderService.getRobotByStatus();
        log.info("扫描到" + robotOrders.size() + "笔过期订单开始处理");
        robotOrders.forEach(robotOrder -> {
            ExchangeOrder exchangeOrder = exchangeOrderService.findOne(robotOrder.getExchangeOrderId());
            if (exchangeOrder.getStatus() != ExchangeOrderStatus.TRADING) {
                robotOrder.setStatus(1);
                robotOrderService.save(robotOrder);
                log.info(robotOrder.getId() + "更新订单状态");
            }
        });
    }

    /**
     * 每10分钟更新三小时后作废订单
     */
    @Test
    public void expiredOrders() {
        List<RobotOrder> robotOrders = robotOrderService.getRobotByStatusAnAndCreateTime();
        String serviceName = "SERVICE-EXCHANGE-API";

        log.info("扫描到" + robotOrders.size() + "笔过期订单开始处理");
        robotOrders.forEach(robotOrder -> {
            try {
                if (robotOrder.getType() == 0) {
                    MessageResult messageResult = robotOrderService.cancelOrder(robotOrder.getMemberId(), robotOrder.getExchangeOrderId());
                    if (messageResult.getCode() == 0) {
                        log.info("结束订单成功");
                        //结束订单后开始对机器人进行处理
                    } else {
                        log.info("结束订单失败");
                        log.info(messageResult.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

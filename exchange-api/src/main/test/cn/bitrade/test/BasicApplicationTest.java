package cn.bitrade.test;

import cn.ztuo.bitrade.ExchangeApiApplication;
import cn.ztuo.bitrade.entity.RobotOrder;
import cn.ztuo.bitrade.service.RobotOrderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

/**
 * @author GS
 * @date 2018年02月06日
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ExchangeApiApplication.class)
public class BasicApplicationTest {

    @Autowired
    private RobotOrderService robotOrderService;

    @Test
    public void test(){
        RobotOrder robotOrder=new RobotOrder();
        robotOrder.setAmount(new BigDecimal("100"));
        robotOrder.setType(0);
        robotOrder.setMemberId(10000L);
        robotOrderService.addRobotOrder(robotOrder);
    }

}

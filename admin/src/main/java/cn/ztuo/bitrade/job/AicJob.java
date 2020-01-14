package cn.ztuo.bitrade.job;

import cn.ztuo.bitrade.dao.SellRobotOrderDao;
import cn.ztuo.bitrade.entity.RobotOrder;
import cn.ztuo.bitrade.entity.SellRobotOrder;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AicJob {

    @Autowired
    private SellRobotOrderDao sellRobotOrderDao;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000*60)
    public void issueJob(){
        //处理sellRobotOrder表里的数据,查询sellRobotOrder表里创建时间+系统参数小于等等钱时间的委托单
    List<SellRobotOrder> sellRobotOrders = sellRobotOrderDao.getSellRobot();
    log.info("扫描到有"+sellRobotOrders.size()+"条订单需要发布");

    sellRobotOrders.forEach(sellRobotOrder -> {
        log.info("机器人发布买单");
        //创建一个机器人订单
        RobotOrder robotOrder = new RobotOrder();
        //设置会员id
        robotOrder.setMemberId(sellRobotOrder.getMemberId());
        //设置投入金额
        robotOrder.setAmount(sellRobotOrder.getAmount());
        //0为买 1为卖
        robotOrder.setType(0);
        //买入的比率
        robotOrder.setRate(sellRobotOrder.getRate());
        //卖出的比率
        robotOrder.setSellRate(sellRobotOrder.getBuyRate());
        //设置卖出的机器人订单
        robotOrder.setSellRobotOrderId(sellRobotOrder.getId());
        kafkaTemplate.send("add-robot-buy-order", JSONObject.toJSONString(robotOrder));
        log.info(" 推送至队列成功");
    });
    }

}

package cn.ztuo.bitrade.config;

import com.alibaba.fastjson.JSON;

import cn.ztuo.bitrade.Trader.CoinTrader;
import cn.ztuo.bitrade.Trader.CoinTraderFactory;
import cn.ztuo.bitrade.entity.ExchangeOrder;
import cn.ztuo.bitrade.entity.ExchangeOrderDetail;
import cn.ztuo.bitrade.service.ExchangeOrderDetailService;
import cn.ztuo.bitrade.service.ExchangeOrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * 在ApplicationListener<ContextRefreshedEvent>使用时，会存在一个问题，在web 项目中（spring mvc），系统会存在两个容器，一个是root application context ,另一个就是我们自己的 projectName-servlet  context（作为root application context的子容器）。这种情况下，就会造成onApplicationEvent方法被执行两次。解决此问题的方法如下：
 *方法一:
 * 可以在类上加入这四个注解的任意一个
 * 1、@controller 控制器（注入服务）
 *
 * 2、@service 服务（注入dao）
 *
 * 3、@repository dao（实现dao访问）
 *
 * 4、@component （把普通pojo实例化到spring容器中，相当于配置文件中的<bean id="" class=""/>）
 *
 * 　  @Component,@Service,@Controller,@Repository注解的类，并把这些类纳入进spring容器中管理。
 * 方法二:@Override
 *   public void onApplicationEvent(ContextRefreshedEvent event) {
 *     if(event.getApplicationContext().getParent() == null){//root application context 没有parent
 *          //需要执行的逻辑代码，当spring容器初始化完成后就会执行该方法。
 *     }
 *   }
 */
//ApplicationListener<ContextRefreshedEvent>：所有bean初始化结束后条用
@Component
public class CoinTraderEvent implements ApplicationListener<ContextRefreshedEvent> {
    private Logger log = LoggerFactory.getLogger(CoinTraderEvent.class);
    @Autowired
    CoinTraderFactory coinTraderFactory;
    @Autowired
    private ExchangeOrderService exchangeOrderService;
    @Autowired
    private ExchangeOrderDetailService exchangeOrderDetailService;
    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    //需要执行的逻辑代码，当spring容器初始化完成后就会执行该方法。
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("======initialize coinTrader======");
        coinTraderFactory.getTraderMap(); //从mysql里面获取所有的交易币
        //所有的交易币
        HashMap<String,CoinTrader> traders = coinTraderFactory.getTraderMap();
        //遍历交易币,进行处理
        traders.forEach((symbol,trader) ->{
            //根据交易对符号查询所有未完成的挂单
            List<ExchangeOrder> orders = exchangeOrderService.findAllTradingOrderBySymbol(symbol);
            //交易订单
            List<ExchangeOrder> tradingOrders = new ArrayList<>();
            //完成的订单
            List<ExchangeOrder> completedOrders = new ArrayList<>();
            //遍历所有未完成的挂单
            orders.forEach(order -> {
                //没有小数点
                BigDecimal tradedAmount = BigDecimal.ZERO;
                BigDecimal turnover = BigDecimal.ZERO;
                //查询某订单的成交详情
                List<ExchangeOrderDetail> details = exchangeOrderDetailService.findAllByOrderId(order.getOrderId());
                //order.setDetail(details);
                //遍历订单详情
                for(ExchangeOrderDetail trade:details){
                    //成交量
                    tradedAmount = tradedAmount.add(trade.getAmount());
                    //成交额，对市价买单有用  multiply:乘
                    turnover = turnover.add(trade.getAmount().multiply(trade.getPrice()));
                }
                //成交量
                order.setTradedAmount(tradedAmount);
                //成交额，对市价买单有用
                order.setTurnover(turnover);
                /**
                 * 订单完成的订单有三种
                 * 1:只要状态不是trading
                 * 2:当是市价买入时,买入量小于成交额
                 * 3:买入或是卖出量小于成交量的
                 * @return
                 */
                if(!order.isCompleted()){
                    //正在交易得订单
                    tradingOrders.add(order);
                }
                else{
                    //交易完成得订单
                    completedOrders.add(order);
                }
            });


            //未完成交易得订单,主动交易输入的订单，交易不完成的会输入到队列
            trader.trade(tradingOrders);

            //判断已完成的订单发送消息通知
            if(completedOrders.size() > 0){
                kafkaTemplate.send("exchange-order-completed", JSON.toJSONString(completedOrders));
            }

            trader.setReady(true);
        });
    }

}

package cn.ztuo.bitrade.consumer;

import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.MemberWalletDao;
import cn.ztuo.bitrade.dao.SellRobotOrderDao;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.service.RobotOrderService;
import cn.ztuo.bitrade.util.MessageResult;
import com.alibaba.fastjson.JSON;

import cn.ztuo.bitrade.Trader.CoinTrader;
import cn.ztuo.bitrade.Trader.CoinTraderFactory;
import com.alibaba.fastjson.JSONObject;
import com.querydsl.core.types.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class ExchangeOrderConsumer {

    @Autowired
    private CoinTraderFactory traderFactory;
    //会员交易
    @Autowired
    private MemberTransactionDao memberTransactionDao;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    //ConsumerRecord:消费者记录
    @KafkaListener(topics = "exchange-order", containerFactory = "kafkaListenerContainerFactory")
    public void onOrderSubmitted(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        //手动确认
        acknowledgment.acknowledge();
        //遍历消费记录
        for (int i = 0; i < records.size(); i++) {
            ConsumerRecord<String, String> record = records.get(i);
            log.info("接收订单>>topic={},value={},size={}", record.topic(), record.value(), records.size());
            //获取交易订单
            ExchangeOrder order = JSON.parseObject(record.value(), ExchangeOrder.class);
            if (order == null) {
                return;
            }
            //是某一个币种的币种信息，所有买卖单信息，盘口信息
            CoinTrader trader = traderFactory.getTrader(order.getSymbol());
            //如果当前币种交易暂停会自动取消订单
            //isTradingHalt:是否暂停交易
            if (trader.isTradingHalt() || !trader.getReady()) {
                //撮合器未准备完成，撤回当前等待的订单
                kafkaTemplate.send("exchange-order-cancel-success", JSON.toJSONString(order));
            } else {
                try {
                    long startTick = System.currentTimeMillis();
                    trader.trade(order);
                    log.info("complete trade,{}ms used!", System.currentTimeMillis() - startTick);
                } catch (Exception e) {

                    e.printStackTrace();
                    log.info("====交易出错，退回订单===", e);
                    kafkaTemplate.send("exchange-order-cancel-success", JSON.toJSONString(order));
                }
            }
        }
    }

    @KafkaListener(topics = "exchange-order-cancel", containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCancel(List<ConsumerRecord<String, String>> records) {
        for (int i = 0; i < records.size(); i++) {
            ConsumerRecord<String, String> record = records.get(i);
            log.info("取消订单topic={},key={},size={}", record.topic(), record.key(), records.size());
            ExchangeOrder order = JSON.parseObject(record.value(), ExchangeOrder.class);
            if (order == null) {
                return;
            }
            CoinTrader trader = traderFactory.getTrader(order.getSymbol());
            if (trader.getReady()) {
                try {
                    ExchangeOrder result = trader.cancelOrder(order);
                    if (result != null) {
                        kafkaTemplate.send("exchange-order-cancel-success", JSON.toJSONString(result));
                    }
                } catch (Exception e) {
                    log.info("====取消订单出错===", e);
                    e.printStackTrace();
                }
            }
        }
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RobotOrderService robotOrderService;

    @Autowired
    private MemberWalletDao memberWalletDao;

    @KafkaListener(topics = "exchange-order-cancel-robot", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onRobotOrderCancel(List<ConsumerRecord<String, String>> records) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //aios当前时价
        BigDecimal aiosRate = (BigDecimal) valueOperations.get(SysConstant.AIOS_RATE);
        for (int i = 0; i < records.size(); i++) {
            ConsumerRecord<String, String> record = records.get(i);
            log.info("取消订单topic={},key={},size={}", record.topic(), record.key(), records.size());
            ExchangeOrder order = JSON.parseObject(record.value(), ExchangeOrder.class);
            if (order == null) {
                return;
            }
            CoinTrader trader = traderFactory.getTrader(order.getSymbol());

            RobotOrder robotOrder = robotOrderService.findByExchangeOrderId(order.getOrderId());
            if (trader.getReady()) {
                try {
                    ExchangeOrder result = trader.cancelOrder(order);
                    if (result != null) {
                        kafkaTemplate.send("exchange-order-cancel-success", JSON.toJSONString(result));
                        //减少付出的冻结的币
                        BigDecimal refundAmount = result.getAmount().subtract(result.getTradedAmount());
                        robotOrder.setEndPurchasePrice(aiosRate);
                        robotOrder.setResidueAmount(refundAmount);
                        robotOrder.setNumberPurchases(robotOrder.getAmount().subtract(refundAmount).multiply(robotOrder.getRate()));
                        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                            log.info("进行退款计算==========");
                            MemberWallet aWallet = memberWalletDao.getMemberWalletByCoinAndMemberId("AIOS", robotOrder.getMemberId());
                            MemberWallet uWallet = memberWalletDao.getMemberWalletByCoinAndMemberId("USDT", robotOrder.getMemberId());
                            //0买1卖
                            if (robotOrder.getType() == 0) {
//                                synchronized (uWallet) {
//                                    robotOrder.setEndQuantity(robotOrder.getResidueAmount().multiply(robotOrder.getEndPurchasePrice()));
                                refundAmount = refundAmount.multiply(robotOrder.getPurchasePrice());
                                log.info("===========解冻的U:{}==================", refundAmount);

                                robotOrder.setBalance(robotOrder.getBalance().subtract(refundAmount));
                                memberWalletDao.thawBalance(uWallet.getId(), refundAmount);
//                                }
                            } else if (robotOrder.getType() == 1) {
//                                synchronized (aWallet) {
                                log.info("===========解冻的A:{}==================", refundAmount);
                                robotOrder.setEndQuantity(robotOrder.getResidueAmount().multiply(robotOrder.getEndPurchasePrice()));
                                memberWalletDao.decreaseFrozen(aWallet.getId(), refundAmount);
                                memberWalletDao.increaseBalance(uWallet.getId(), robotOrder.getEndQuantity());
                                log.info("===========转换的U:{}==================", robotOrder.getEndQuantity());
                                insertDetile(robotOrder.getEndQuantity(), robotOrder.getMemberId(), "USDT", TransactionType.CANCELLATION_OF_ORDER);
//                                }
                            } else {
                                return;
                            }
                            robotOrder.setStatus(2);
                            robotOrderService.save(robotOrder);
                        }
                    }
                } catch (Exception e) {
                    log.info("====取消订单出错===", e);
                    e.printStackTrace();
                }
            }

        }
    }

    @Autowired
    private SellRobotOrderDao sellRobotOrderDao;

    @KafkaListener(topics = "add-robot-sell-order", containerFactory = "kafkaListenerContainerFactory")
    public void addRobotSellOrder(List<ConsumerRecord<String, String>> records) {
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < records.size(); i++) {
            ConsumerRecord<String, String> record = records.get(i);
            log.info("发送卖单topic={},key={},size={}", record.topic(), record.key(), records.size());
            RobotOrder order = JSON.parseObject(record.value(), RobotOrder.class);
            RobotOrder robotOrder = robotOrderService.findByExchangeOrderId(order.getExchangeOrderId());
            if (robotOrder != null) {
                log.info("发送卖单==========================================");
                RobotOrder robotOrderNew = new RobotOrder();
                if (robotOrder.getType() == 0) {
                    robotOrderNew.setMemberId(robotOrder.getMemberId());
                    robotOrderNew.setAmount(order.getAmount());
                    robotOrderNew.setType(1);
                    robotOrderNew.setRate(robotOrder.getSellRate());
                    robotOrderNew.setBuyRate(robotOrder.getPurchasePrice());
                    log.info("order======={}", robotOrderNew);
                    MessageResult messageResult = robotOrderService.addRobotOrder(robotOrderNew);
                    if ( messageResult.getCode() == 0  ) {
                        log.info("机器人订单 " + robotOrderNew.getId() + " 发布卖单======================>");
                        log.info(messageResult.getMessage());
                    }
                }
            } else {
                log.info("orderId" + order.getExchangeOrderId() + "订单不是机器人发布委托");
            }

        }
    }

    @KafkaListener(topics = "add-robot-buy-order", containerFactory = "kafkaListenerContainerFactory")
    public void addRobotBuyOrder(List<ConsumerRecord<String, String>> records) {
        for (int i = 0; i < records.size(); i++) {
            ConsumerRecord<String, String> record = records.get(i);
            log.info("发送买单topic={},key={},size={}", record.topic(), record.key(), records.size());
            RobotOrder order = JSON.parseObject(record.value(), RobotOrder.class);
            log.info("order======={}", order);
            //0为买 1为卖
            if (order.getType() == 0) {
                //查询sellBobotOrder表里的委托单
                SellRobotOrder sellRobotOrder = sellRobotOrderDao.getOrderById(order.getSellRobotOrderId());
                //插入到robotOrder表中
                MessageResult messageResult = robotOrderService.addRobotOrder(order);
                if (messageResult.getCode() == 0) {
                    sellRobotOrder.setStatus(1);
                    sellRobotOrderDao.save(sellRobotOrder);
                    log.info("机器人订单 " + order.getId() + " 发布买单失败原因为======================>");
                    log.info(messageResult.getMessage());
                } else {
                    sellRobotOrder.setStatus(1);
                    sellRobotOrderDao.save(sellRobotOrder);
                }
            } else {
                log.info("只允许发布买单");
            }
        }
    }

    /**
     * 功能描述:保存明细
     *
     * @param:
     * @return:
     */
    public void insertDetile(BigDecimal price, Long memberId, String currency, TransactionType type) {
        //插入明细
        MemberTransaction memberTransaction = new MemberTransaction();
        memberTransaction.setAmount(price);
        memberTransaction.setFee(BigDecimal.ZERO);
        memberTransaction.setMemberId(memberId);
        memberTransaction.setSymbol(currency);
        memberTransaction.setType(type);
        memberTransactionDao.save(memberTransaction);
    }
}

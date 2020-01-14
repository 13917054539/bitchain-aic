package cn.ztuo.bitrade.consumer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.ztuo.bitrade.dao.SellRobotOrderDao;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.service.RobotOrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

import cn.ztuo.bitrade.constant.NettyCommand;
import cn.ztuo.bitrade.handler.NettyHandler;
import cn.ztuo.bitrade.job.ExchangePushJob;
import cn.ztuo.bitrade.processor.CoinProcessor;
import cn.ztuo.bitrade.processor.CoinProcessorFactory;
import cn.ztuo.bitrade.service.ExchangeOrderService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExchangeTradeConsumer {
	private Logger logger = LoggerFactory.getLogger(ExchangeTradeConsumer.class);
	@Autowired
	private CoinProcessorFactory coinProcessorFactory;
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	@Autowired
	private ExchangeOrderService exchangeOrderService;
	@Autowired
	private NettyHandler nettyHandler;
	@Value("${second.referrer.award}")
	private boolean secondReferrerAward;
	private ExecutorService executor = new ThreadPoolExecutor(30, 100, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(1024), new ThreadPoolExecutor.AbortPolicy());
	@Autowired
	private ExchangePushJob pushJob;

	/**
	 * 处理成交明细
	 *
	 * @param records
	 */
	@KafkaListener(topics = "exchange-trade", containerFactory = "kafkaListenerContainerFactory")
	public void handleTrade(List<ConsumerRecord<String, String>> records) {
		for (int i = 0; i < records.size(); i++) {
			ConsumerRecord<String, String> record = records.get(i);
			executor.submit(new HandleTradeThread(record));
		}
	}

	@Autowired
    private RobotOrderService robotOrderService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SellRobotOrderDao sellRobotOrderDao;

	/**
	 * 处理交易完成的订单
	 * @param records
	 */
    @KafkaListener(topics = "exchange-order-completed", containerFactory = "kafkaListenerContainerFactory")
	public void handleOrderCompleted(List<ConsumerRecord<String, String>> records) {
		try {
			for (int i = 0; i < records.size(); i++) {
				//获得生产者信息
				ConsumerRecord<String, String> record = records.get(i);
				logger.info("订单交易处理完成消息topic={},value={}", record.topic(), record.value());
				//解析成ExchangeOrder的集合
				List<ExchangeOrder> orders = JSON.parseArray(record.value(), ExchangeOrder.class);
				//遍历
				for (ExchangeOrder order : orders) {
					//获得交易对符号
					String symbol = order.getSymbol();

					//下一步
					// 委托成交完成处理
					exchangeOrderService.tradeCompleted(order.getOrderId(), order.getTradedAmount(),
							order.getTurnover());


					//查询是否是机器人订单
                    RobotOrder robotOrder=robotOrderService.findByExchangeOrderId(order.getOrderId());
                    //如果是
					if(robotOrder!=null){
						//判断订单状态,0为买 1为卖
                        if (robotOrder.getType() == 1) {
                        	//根据会员id查询机器人订单
                        	List list=robotOrderService.getRobotOrderByMemberId(robotOrder.getMemberId());
                        	//如果没有该会员得订单
                            if (list==null||list.size()==0) {
                            	//根据会员id查询会员
                                Member member = memberService.findOne(robotOrder.getMemberId());
                                //机器人状态
                                if (member.getRobotStatus() == 1) {
                                	//卖出机器单
                                    SellRobotOrder sellRobotOrder = new SellRobotOrder();
                                    //会员id
                                    sellRobotOrder.setMemberId(robotOrder.getMemberId());
                                    //投入金额
                                    sellRobotOrder.setAmount(member.getRobotLimit());
                                    //买率
                                    sellRobotOrder.setBuyRate(new BigDecimal(0.0041));
									//买入或者卖出的比率
                                    sellRobotOrder.setRate(BigDecimal.ZERO);
                                    sellRobotOrder.setCreateTime(new Date());
									//状态 0为启动中 ,1为已关闭 ,2
                                    sellRobotOrder.setStatus(0);
                                    sellRobotOrderDao.save(sellRobotOrder);
                                } else {
                                    log.info("用户" + member.getMobilePhone() + "已关闭机器人");
                                }
                            }else{
								log.info(list+"=============================");
							}
                        }
                    }else{
                        log.info("orderId" + order.getOrderId() + "订单不是机器人发布委托");
                    }
					// 推送订单成交
					messagingTemplate.convertAndSend(
							"/topic/market/order-completed/" + symbol + "/" + order.getMemberId(), order);
					nettyHandler.handleOrder(NettyCommand.PUSH_EXCHANGE_ORDER_COMPLETED, order);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 处理模拟交易
	 *
	 * @param records
	 */
	@KafkaListener(topics = "exchange-trade-mocker", containerFactory = "kafkaListenerContainerFactory")
	public void handleMockerTrade(List<ConsumerRecord<String, String>> records) {
		try {
			for (int i = 0; i < records.size(); i++) {
				ConsumerRecord<String, String> record = records.get(i);
				logger.info("mock数据topic={},value={},size={}", record.topic(), record.value(), records.size());
				List<ExchangeTrade> trades = JSON.parseArray(record.value(), ExchangeTrade.class);
				String symbol = trades.get(0).getSymbol();
				// 处理行情
				CoinProcessor coinProcessor = coinProcessorFactory.getProcessor(symbol);
				if (coinProcessor != null) {
					coinProcessor.process(trades);
				}
				pushJob.addTrades(symbol, trades);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 消费交易盘口信息
	 *
	 * @param records
	 */
	@KafkaListener(topics = "exchange-trade-plate", containerFactory = "kafkaListenerContainerFactory")
	public void handleTradePlate(List<ConsumerRecord<String, String>> records) {
		try {
			for (int i = 0; i < records.size(); i++) {
				ConsumerRecord<String, String> record = records.get(i);
				logger.info("推送盘口信息topic={},value={},size={}", record.topic(), record.value(), records.size());
				TradePlate plate = JSON.parseObject(record.value(), TradePlate.class);
				String symbol = plate.getSymbol();
				pushJob.addPlates(symbol, plate);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 订单取消成功
	 *
	 * @param records
	 */
	@KafkaListener(topics = "exchange-order-cancel-success", containerFactory = "kafkaListenerContainerFactory")
	public void handleOrderCanceled(List<ConsumerRecord<String, String>> records) {
		try {
			for (int i = 0; i < records.size(); i++) {
				ConsumerRecord<String, String> record = records.get(i);
				logger.info("取消订单消息topic={},value={},size={}", record.topic(), record.value(), records.size());
				ExchangeOrder order = JSON.parseObject(record.value(), ExchangeOrder.class);
				String symbol = order.getSymbol();
				// 调用服务处理
				exchangeOrderService.cancelOrder(order.getOrderId(), order.getTradedAmount(), order.getTurnover());
				// 推送实时成交
				messagingTemplate.convertAndSend("/topic/market/order-canceled/" + symbol + "/" + order.getMemberId(),
						order);
				nettyHandler.handleOrder(NettyCommand.PUSH_EXCHANGE_ORDER_CANCELED, order);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class HandleTradeThread implements Runnable {
		private ConsumerRecord<String, String> record;

		private HandleTradeThread(ConsumerRecord<String, String> record) {
			this.record = record;
		}

		@Override
		public void run() {
			logger.info("topic={},value={}", record.topic(), record.value());
			try {
				List<ExchangeTrade> trades = JSON.parseArray(record.value(), ExchangeTrade.class);
				String symbol = trades.get(0).getSymbol();
				CoinProcessor coinProcessor = coinProcessorFactory.getProcessor(symbol);
				for (ExchangeTrade trade : trades) {
					// 成交明细处理
					exchangeOrderService.processExchangeTrade(trade, secondReferrerAward);
					// 推送订单成交订阅
					ExchangeOrder buyOrder = exchangeOrderService.findOne(trade.getBuyOrderId());
					ExchangeOrder sellOrder = exchangeOrderService.findOne(trade.getSellOrderId());
					messagingTemplate.convertAndSend(
							"/topic/market/order-trade/" + symbol + "/" + buyOrder.getMemberId(), buyOrder);
					messagingTemplate.convertAndSend(
							"/topic/market/order-trade/" + symbol + "/" + sellOrder.getMemberId(), sellOrder);
					nettyHandler.handleOrder(NettyCommand.PUSH_EXCHANGE_ORDER_TRADE, buyOrder);
					nettyHandler.handleOrder(NettyCommand.PUSH_EXCHANGE_ORDER_TRADE, sellOrder);
				}
				// 处理K线行情
				if (coinProcessor != null) {
					coinProcessor.process(trades);
				}
				pushJob.addTrades(symbol, trades);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}



}

package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.*;
import cn.ztuo.bitrade.dao.SellRobotOrderDao;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.sparkframework.sql.DB;

import cn.ztuo.bitrade.dao.ExchangeOrderDetailRepository;
import cn.ztuo.bitrade.dao.ExchangeOrderRepository;
import cn.ztuo.bitrade.dao.OrderDetailAggregationRepository;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.pagination.Criteria;
import cn.ztuo.bitrade.pagination.PageResult;
import cn.ztuo.bitrade.pagination.Restrictions;
import cn.ztuo.bitrade.service.Base.BaseService;
import cn.ztuo.bitrade.util.BigDecimalUtils;
import cn.ztuo.bitrade.util.DateUtil;
import cn.ztuo.bitrade.util.GeneratorUtil;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ExchangeOrderService extends BaseService {

    @Autowired
    private ExchangeOrderRepository exchangeOrderRepository;
    @Autowired
    private MemberWalletService memberWalletService;
    @Autowired
    private ExchangeOrderDetailRepository exchangeOrderDetailRepository;
    @Autowired
    private ExchangeCoinService exchangeCoinService;
    @Autowired
    private OrderDetailAggregationRepository orderDetailAggregationRepository;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberTransactionService transactionService;
    @Autowired
    private RewardPromotionSettingService rewardPromotionSettingService;
    @Autowired
    private RewardRecordService rewardRecordService;
    @Autowired
    private ExchangeOrderDetailService exchangeOrderDetailService;
    @Autowired
    private ExchangeReleaseTokenService exchangeReleaseTokenService;
    @Autowired
    private RobotOrderService robotOrderService;


    @Value("${channel.enable:false}")
    private Boolean channelEnable;
    @Value("${channel.exchange-rate:0.00}")
    private BigDecimal channelExchangeRate;

    @Autowired
    private LocaleMessageSourceService msService;

    public Page<ExchangeOrder> findAll(Predicate predicate, Pageable pageable) {
        return exchangeOrderRepository.findAll(predicate, pageable);
    }


    /**
     * 添加委托订单, 冻结资金，订单存入exchangeOrder表
     *
     * @param memberId
     * @param order
     * @return
     */
    @Transactional
    public MessageResult addOrder(Long memberId, ExchangeOrder order) {

        order.setTime(Calendar.getInstance().getTimeInMillis());
        order.setStatus(ExchangeOrderStatus.TRADING);
        order.setTradedAmount(BigDecimal.ZERO);
        order.setOrderId(GeneratorUtil.getOrderId("E"));
        log.info("add order:{}", order);
        if (order.getDirection() == ExchangeOrderDirection.BUY) {
            MemberWallet wallet = memberWalletService.findByCoinUnitAndMemberId(order.getBaseSymbol(), memberId);
            if (wallet.getIsLock().equals(BooleanEnum.IS_TRUE)) {
                return MessageResult.error("钱包已锁定");
            }
            BigDecimal turnover;
            if (order.getType() == ExchangeOrderType.MARKET_PRICE) {
                turnover = order.getAmount();
            } else {
                turnover = order.getAmount().multiply(order.getPrice());
            }
            if (wallet.getBalance().compareTo(turnover) < 0) {
                return MessageResult.error(500, "" +
                        "" + order.getBaseSymbol());
            } else {
                MessageResult result = memberWalletService.freezeBalance(wallet, turnover);
                if (result.getCode() != 0) {
                    return MessageResult.error(500, msService.getMessage("INSUFFICIENT_COIN") + order.getBaseSymbol());
                }
            }
        } else if (order.getDirection() == ExchangeOrderDirection.SELL) {
            MemberWallet wallet = memberWalletService.findByCoinUnitAndMemberId(order.getCoinSymbol(), memberId);
            if (wallet.getIsLock().equals(BooleanEnum.IS_TRUE)) {
                return MessageResult.error("钱包已锁定");
            }
            if (wallet.getBalance().compareTo(order.getAmount()) < 0) {
                return MessageResult.error(500, msService.getMessage("INSUFFICIENT_COIN") + order.getCoinSymbol());
            } else {
                MessageResult result = memberWalletService.freezeBalance(wallet, order.getAmount());
                if (result.getCode() != 0) {
                    return MessageResult.error(500, msService.getMessage("INSUFFICIENT_COIN") + order.getCoinSymbol());
                }
            }
        }
        order = exchangeOrderRepository.saveAndFlush(order);
        if (order != null) {
            return MessageResult.success("success");
        } else {
            return MessageResult.error(500, "error");
        }
    }

    /**
     * @param uid
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ExchangeOrder> findHistory(Long uid, String symbol, int pageNo, int pageSize) {
        Sort orders = new Sort(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = new PageRequest(pageNo, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, true));
        specification.add(Restrictions.eq("memberId", uid, true));
        specification.add(Restrictions.ne("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.findAll(specification, pageRequest);
    }

    /**
     * 个人中心历史委托
     *
     * @param uid
     * @param symbol
     * @param type
     * @param status
     * @param startTime
     * @param endTime
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ExchangeOrder> findPersonalHistory(Long uid, String symbol, ExchangeOrderType type, ExchangeOrderStatus status, String startTime, String endTime, ExchangeOrderDirection direction, int pageNo, int pageSize) {
        Sort orders = new Sort(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = new PageRequest(pageNo - 1, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        if (StringUtils.isNotEmpty(symbol)) {
            specification.add(Restrictions.eq("symbol", symbol, true));
        }
        if (type != null && StringUtils.isNotEmpty(type.toString())) {
            specification.add(Restrictions.eq("type", type, true));
        }
        if (direction != null && StringUtils.isNotEmpty(direction.toString())) {
            specification.add(Restrictions.eq("direction", direction, true));
        }
        specification.add(Restrictions.eq("memberId", uid, true));
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            specification.add(Restrictions.gte("time", Long.valueOf(startTime), true));
            specification.add(Restrictions.lte("time", Long.valueOf(endTime), true));
        }

        if (status == null) {
            specification.add(Restrictions.ne("status", ExchangeOrderStatus.TRADING, false));
        } else {
            specification.add(Restrictions.eq("status", status, true));
        }

        return exchangeOrderRepository.findAll(specification, pageRequest);
    }


    /**
     * 个人中心当前委托
     *
     * @param uid
     * @param symbol
     * @param type
     * @param startTime
     * @param endTime
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ExchangeOrder> findPersonalCurrent(Long uid, String symbol, ExchangeOrderType type, String startTime, String endTime, ExchangeOrderDirection direction, int pageNo, int pageSize) {
        Sort orders = new Sort(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = new PageRequest(pageNo - 1, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        if (StringUtils.isNotEmpty(symbol)) {
            specification.add(Restrictions.eq("symbol", symbol, true));
        }
        if (type != null && StringUtils.isNotEmpty(type.toString())) {
            specification.add(Restrictions.eq("type", type, true));
        }
        specification.add(Restrictions.eq("memberId", uid, false));
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            specification.add(Restrictions.gte("time", Long.valueOf(startTime), true));
            specification.add(Restrictions.lte("time", Long.valueOf(endTime), true));
        }
        if (direction != null && StringUtils.isNotEmpty(direction.toString())) {
            specification.add(Restrictions.eq("direction", direction, true));
        }
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.findAll(specification, pageRequest);
    }

    /**
     * 查询当前交易中的委托
     *
     * @param uid
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ExchangeOrder> findCurrent(Long uid, String symbol, int pageNo, int pageSize) {
        Sort orders = new Sort(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = new PageRequest(pageNo, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, true));
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.findAll(specification, pageRequest);
    }

    @Autowired
    private SellRobotOrderDao sellRobotOrderDao;


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 处理交易匹配
     *
     * @param trade
     * @param secondReferrerAward 二级推荐人是否返回佣金 true 返回佣金
     * @return
     * @throws Exception
     */
    @Transactional
    public MessageResult processExchangeTrade(ExchangeTrade trade, boolean secondReferrerAward) throws Exception {
        log.info("processExchangeTrade,trade = {}", trade);
        if (trade == null || trade.getBuyOrderId() == null || trade.getSellOrderId() == null) {
            return MessageResult.error(500, "trade is null");
        }
        ExchangeOrder buyOrder = exchangeOrderRepository.findByOrderId(trade.getBuyOrderId());
        ExchangeOrder sellOrder = exchangeOrderRepository.findByOrderId(trade.getSellOrderId());
        if (buyOrder == null || sellOrder == null) {
            log.error("order not found");
            return MessageResult.error(500, "order not found");
        }
        //获取手续费率
        ExchangeCoin coin = exchangeCoinService.findBySymbol(buyOrder.getSymbol());
        if (coin == null) {
            log.error("invalid trade symbol {}", buyOrder.getSymbol());
            return MessageResult.error(500, "invalid trade symbol {}" + buyOrder.getSymbol());
        }
        //根据memberId锁表，防止死锁
        DB.query("select id from member_wallet where member_id = ? for update;", buyOrder.getMemberId());
        if (!buyOrder.getMemberId().equals(sellOrder.getMemberId())) {
            DB.query("select id from member_wallet where member_id = ? for update;", sellOrder.getMemberId());
        }

        //处理买入订单 手续费是交易币  交易币对usdtRate
        processOrder(buyOrder, trade, coin, secondReferrerAward);
        //处理卖出订单 手续费是基准币 基准币对usdtRate
        processOrder(sellOrder, trade, coin, secondReferrerAward);
        try {
            if (buyOrder.getSymbol().equals("AIOS/USDT")) {
                if (buyOrder.getMemberId() != 10000L && buyOrder.getMemberId() != 13491L && buyOrder.getMemberId() != 13545L) {
                    log.info("发送卖单==========================");
                    RobotOrder robotOrderNew = new RobotOrder();
                    BigDecimal fee = trade.getAmount().multiply(coin.getFee());
                    robotOrderNew.setAmount(trade.getAmount().subtract(fee));
                    robotOrderNew.setExchangeOrderId(buyOrder.getOrderId());
                    kafkaTemplate.send("add-robot-sell-order", JSONObject.toJSONString(robotOrderNew));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("处理延迟卖单异常");
        }
        return MessageResult.success("process success");
    }


    /**
     * 对发生交易的委托处理相应的钱包
     *
     * @param order               委托订单
     * @param trade               交易详情
     * @param coin                交易币种信息，包括手续费 交易币种信息等等
     * @param secondReferrerAward 二级推荐人是否返佣
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void processOrder(ExchangeOrder order, ExchangeTrade trade, ExchangeCoin coin, boolean secondReferrerAward) {
        try {
            Long time = Calendar.getInstance().getTimeInMillis();
            //添加成交详情
            ExchangeOrderDetail orderDetail = new ExchangeOrderDetail();
            orderDetail.setOrderId(order.getOrderId());
            orderDetail.setTime(time);
            orderDetail.setPrice(trade.getPrice());
            orderDetail.setAmount(trade.getAmount());


            BigDecimal incomeCoinAmount, turnover, outcomeCoinAmount;
            if (order.getDirection() == ExchangeOrderDirection.BUY) {
                turnover = trade.getBuyTurnover();
            } else {
                turnover = trade.getSellTurnover();
            }
            orderDetail.setTurnover(turnover);
            //手续费，买入订单收取coin,卖出订单收取baseCoin
            BigDecimal fee;
            if (order.getDirection() == ExchangeOrderDirection.BUY) {
                fee = trade.getAmount().multiply(coin.getFee());
            } else {
                fee = turnover.multiply(coin.getFee());
            }
            orderDetail.setFee(fee);
            exchangeOrderDetailRepository.save(orderDetail);  //order存入mongoDB


            /**
             * 基于时间的聚合（aggregation）币币交易订单及手续费明细存入mongodb
             *
             */
            OrderDetailAggregation aggregation = new OrderDetailAggregation();
            aggregation.setType(OrderTypeEnum.EXCHANGE);
            aggregation.setAmount(order.getAmount().doubleValue());
            aggregation.setFee(orderDetail.getFee().doubleValue());
            aggregation.setTime(orderDetail.getTime());
            aggregation.setDirection(order.getDirection());
            aggregation.setOrderId(order.getOrderId());
            if (order.getDirection() == ExchangeOrderDirection.BUY) {
                aggregation.setUnit(order.getBaseSymbol());
            } else {
                aggregation.setUnit(order.getCoinSymbol());
            }
            Member member = memberService.findOne(order.getMemberId());
            if (member != null) {
                aggregation.setMemberId(member.getId());
                aggregation.setUsername(member.getUsername());
                aggregation.setRealName(member.getRealName());
            }
            orderDetailAggregationRepository.save(aggregation); //聚合order存入mongoDB



            //处理账户增加的币数，买入的时候获得交易币，卖出的时候获得基币
            if (order.getDirection() == ExchangeOrderDirection.BUY) {
                incomeCoinAmount = trade.getAmount().subtract(fee);
            } else {
                incomeCoinAmount = turnover.subtract(fee);
            }
            String incomeSymbol = order.getDirection() == ExchangeOrderDirection.BUY ? order.getCoinSymbol() : order.getBaseSymbol();
            MemberWallet incomeWallet = memberWalletService.findByCoinUnitAndMemberId(incomeSymbol, order.getMemberId());
            memberWalletService.increaseBalance(incomeWallet.getId(), incomeCoinAmount);
            //扣除账户减少的币数，买入的时候算成交额，卖出的算成交量
            String outcomeSymbol = order.getDirection() == ExchangeOrderDirection.BUY ? order.getBaseSymbol() : order.getCoinSymbol();
            if (order.getDirection() == ExchangeOrderDirection.BUY) {
                outcomeCoinAmount = turnover;
            } else {
                outcomeCoinAmount = trade.getAmount();
            }
            MemberWallet outcomeWallet = memberWalletService.findByCoinUnitAndMemberId(outcomeSymbol, order.getMemberId());
            log.info("===========================减少冻结金额==========================================");
            //减少冻结余额
            memberWalletService.decreaseFrozen(outcomeWallet.getId(), outcomeCoinAmount);
            //增加资金的记录
            MemberTransaction transaction = new MemberTransaction();
            transaction.setAmount(incomeCoinAmount);
            transaction.setSymbol(incomeSymbol);
            transaction.setAddress("");
            transaction.setMemberId(incomeWallet.getMemberId());
            transaction.setType(TransactionType.EXCHANGE);
            //原手续费
            transaction.setFee(fee);
            //折扣手续费
            transaction.setDiscountFee("0");
            //实收手续费
            transaction.setRealFee(fee.toString());
            transactionService.save(transaction);

            //减少资金的记录
            MemberTransaction transaction2 = new MemberTransaction();
            transaction2.setAmount(outcomeCoinAmount.negate());
            transaction2.setSymbol(outcomeSymbol);
            transaction2.setAddress("");
            transaction2.setMemberId(incomeWallet.getMemberId());
            transaction2.setType(TransactionType.EXCHANGE);
            transaction2.setFee(BigDecimal.ZERO);
            transaction2.setRealFee("0");
            transaction2.setDiscountFee("0");
            transactionService.save(transaction2);
           /* try {
                //威尔交易所 买单交易手续费返佣金
                if(order.getDirection() == ExchangeOrderDirection.BUY){
                    promoteReward(fee, member, incomeSymbol, secondReferrerAward);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("发放币币交易推广手续费佣金出错");
            }*/
           /* try {
                //威尔交易所 买单交易释放平台币
                *//*if(order.getDirection() == ExchangeOrderDirection.BUY){
                    releaseToken(member, incomeSymbol,order.getOrderId(),trade.getAmount());
                }*//*
            } catch (Exception e) {
                e.printStackTrace();
                log.error("币币交易释放平台币出错");
            }*/
        } catch (Exception e) {
            log.info(">>>>>处理交易明细出错>>>>>>>>>{}", e);
            e.printStackTrace();
        }
    }


    /**
     * 释放平台代币
     * */
    /* public void releaseToken(Member member,String incomeSymbol,String orderId,BigDecimal amount){
     */

    /**
     * 检查当前用户今日有没有释放代币的名额
     * 每天交易只能释放五次
     *//*
        List<ExchangeReleaseToken>  exchangeReleaseTokenList = exchangeReleaseTokenService.findByReleaseTimeBetweenAndMemberIdIs(DateUtil.getNowStartTime().getTime(),
                DateUtil.getNowEndTime().getTime(),member.getId());
        if(exchangeReleaseTokenList.size()>=5){
            return;
        }
        MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId(incomeSymbol, member.getId());
        if(memberWallet.getToReleased().compareTo(BigDecimal.ONE)<0){
            return;
        }
        if(amount.compareTo(new BigDecimal("10000"))>=0){
            amount = new BigDecimal("10000");
        }
        ExchangeReleaseToken exchangeReleaseToken = new ExchangeReleaseToken();
        exchangeReleaseToken.setReleaseTime(System.currentTimeMillis());
        exchangeReleaseToken.setReleaseAmount(amount.multiply(BigDecimalUtils.getRate(new BigDecimal("1"))));
        exchangeReleaseToken.setOrderId(orderId);
        exchangeReleaseToken.setMemberId(member.getId());
        exchangeReleaseToken.setBalance(memberWallet.getToReleased().subtract(exchangeReleaseToken.getReleaseAmount()));
        exchangeReleaseTokenService.save(exchangeReleaseToken);
        memberWalletService.increaseBalanceAndToReleased(memberWallet.getId(), exchangeReleaseToken.getReleaseAmount());
    }*/
    public List<ExchangeOrderDetail> getAggregation(String orderId) {
        return exchangeOrderDetailService.findAllByOrderId(orderId);
    }


    /**
     * 渠道币币交易返佣  新版本 方案
     */
//    public void processChannelReward(Member member,String symbol,BigDecimal fee){
//        MemberWallet channelWallet =  memberWalletService.findByCoinUnitAndMemberId(symbol,member.getChannelId());
//        if(channelWallet != null && fee.compareTo(BigDecimal.ZERO) > 0 ){
//            BigDecimal amount = fee.multiply(channelExchangeRate);
//            memberWalletService.increaseBalance(channelWallet.getId(),amount);
//            MemberTransaction memberTransaction = new MemberTransaction();
//            memberTransaction.setAmount(amount);
//            memberTransaction.setFee(BigDecimal.ZERO);
//            memberTransaction.setMemberId(member.getChannelId());
//            memberTransaction.setSymbol(symbol);
//            memberTransaction.setType(TransactionType.CHANNEL_AWARD);
//            transactionService.save(memberTransaction);
//        }
//    }

    /**
     * 交易手续费返佣金
     *
     * @param fee                 手续费
     * @param member              订单拥有者
     * @param incomeSymbol        币种
     * @param secondReferrerAward 二级推荐人是否返佣控制
     */
    public void promoteReward(BigDecimal fee, Member member, String incomeSymbol, boolean secondReferrerAward) {
        RewardPromotionSetting rewardPromotionSetting = rewardPromotionSettingService.findByType(PromotionRewardType.EXCHANGE_TRANSACTION);
        if (rewardPromotionSetting != null && member.getInviterId() != null) {
            if (!(DateUtil.diffDays(new Date(), member.getRegistrationTime()) > rewardPromotionSetting.getEffectiveTime())) {
                Member member1 = memberService.findOne(member.getInviterId());
                MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId(incomeSymbol, member1.getId());
                JSONObject jsonObject = JSONObject.parseObject(rewardPromotionSetting.getInfo());
                BigDecimal reward = BigDecimalUtils.mulRound(fee, BigDecimalUtils.getRate(jsonObject.getBigDecimal("one")), 8);
                if (reward.compareTo(BigDecimal.ZERO) > 0) {
                    memberWalletService.increaseBalance(memberWallet.getId(), reward);
                    MemberTransaction memberTransaction = new MemberTransaction();
                    memberTransaction.setAmount(reward);
                    memberTransaction.setFee(BigDecimal.ZERO);
                    memberTransaction.setMemberId(member1.getId());
                    memberTransaction.setSymbol(incomeSymbol);
                    memberTransaction.setType(TransactionType.PROMOTION_AWARD);
                    memberTransaction.setDiscountFee("0");
                    memberTransaction.setRealFee("0");
                    memberTransaction = transactionService.save(memberTransaction);
                    RewardRecord rewardRecord1 = new RewardRecord();
                    rewardRecord1.setAmount(reward);
                    rewardRecord1.setCoin(memberWallet.getCoin());
                    rewardRecord1.setMember(member1);
                    rewardRecord1.setRemark(rewardPromotionSetting.getType().getCnName());
                    rewardRecord1.setType(RewardRecordType.PROMOTION);
                    rewardRecordService.save(rewardRecord1);
                }

                // 控制推荐人推荐是否返佣 等于false是二级推荐人不返佣
             /*   if (secondReferrerAward == false) {
                    log.info("控制字段 : secondReferrerAward ={} , 跳过二级推荐人返佣", secondReferrerAward);
                    return;
                }*/
                if (member1.getInviterId() != null && !(DateUtil.diffDays(new Date(), member1.getRegistrationTime()) > rewardPromotionSetting.getEffectiveTime())) {
                    Member member2 = memberService.findOne(member1.getInviterId());
                    MemberWallet memberWallet1 = memberWalletService.findByCoinUnitAndMemberId(incomeSymbol, member2.getId());
                    BigDecimal reward1 = BigDecimalUtils.mulRound(fee, BigDecimalUtils.getRate(jsonObject.getBigDecimal("two")), 8);
                    if (reward1.compareTo(BigDecimal.ZERO) > 0) {
                        //memberWallet1.setBalance(BigDecimalUtils.add(memberWallet1.getBalance(), reward));
                        memberWalletService.increaseBalance(memberWallet1.getId(), reward1);
                        MemberTransaction memberTransaction = new MemberTransaction();
                        memberTransaction.setAmount(reward1);
                        memberTransaction.setFee(BigDecimal.ZERO);
                        memberTransaction.setMemberId(member2.getId());
                        memberTransaction.setSymbol(incomeSymbol);
                        memberTransaction.setType(TransactionType.PROMOTION_AWARD);
                        transactionService.save(memberTransaction);
                        RewardRecord rewardRecord1 = new RewardRecord();
                        rewardRecord1.setAmount(reward1);
                        rewardRecord1.setCoin(memberWallet1.getCoin());
                        rewardRecord1.setMember(member2);
                        rewardRecord1.setRemark(rewardPromotionSetting.getType().getCnName());
                        rewardRecord1.setType(RewardRecordType.PROMOTION);
                        rewardRecordService.save(rewardRecord1);
                    }

                    /**
                     * 三级推荐人返利
                     * */
                    if (member2.getInviterId() != null && !(DateUtil.diffDays(new Date(), member2.getRegistrationTime()) > rewardPromotionSetting.getEffectiveTime())) {
                        Member member3 = memberService.findOne(member2.getInviterId());
                        MemberWallet memberWallet2 = memberWalletService.findByCoinUnitAndMemberId(incomeSymbol, member3.getId());
                        BigDecimal reward2 = BigDecimalUtils.mulRound(fee, BigDecimalUtils.getRate(jsonObject.getBigDecimal("three")), 8);
                        if (reward1.compareTo(BigDecimal.ZERO) > 0) {
                            memberWalletService.increaseBalance(memberWallet2.getId(), reward2);
                            MemberTransaction memberTransaction = new MemberTransaction();
                            memberTransaction.setAmount(reward2);
                            memberTransaction.setFee(BigDecimal.ZERO);
                            memberTransaction.setMemberId(member3.getId());
                            memberTransaction.setSymbol(incomeSymbol);
                            memberTransaction.setType(TransactionType.PROMOTION_AWARD);
                            transactionService.save(memberTransaction);
                            RewardRecord rewardRecord1 = new RewardRecord();
                            rewardRecord1.setAmount(reward2);
                            rewardRecord1.setCoin(memberWallet1.getCoin());
                            rewardRecord1.setMember(member3);
                            rewardRecord1.setRemark(rewardPromotionSetting.getType().getCnName());
                            rewardRecord1.setType(RewardRecordType.PROMOTION);
                            rewardRecordService.save(rewardRecord1);
                        }
                    }
                }
            }
        }
    }

    /**
     * 查询所有未完成的挂单
     *
     * @param symbol 交易对符号
     * @return
     */
    public List<ExchangeOrder> findAllTradingOrderBySymbol(String symbol) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        //限制条件
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.findAll(specification);
    }

    @Override
    public List<ExchangeOrder> findAll() {
        return exchangeOrderRepository.findAll();
    }

    public ExchangeOrder findOne(String id) {
        return exchangeOrderRepository.findOne(id);
    }

    @Transactional(readOnly = true)
    public PageResult<ExchangeOrder> queryWhereOrPage(List<Predicate> predicates, Integer pageNo, Integer pageSize) {
        List<ExchangeOrder> list;
        JPAQuery<ExchangeOrder> jpaQuery = queryFactory.selectFrom(QExchangeOrder.exchangeOrder);
        if (predicates != null) {
            jpaQuery.where(predicates.toArray(new BooleanExpression[predicates.size()]));
        }
        jpaQuery.orderBy(QExchangeOrder.exchangeOrder.time.desc());
        if (pageNo != null && pageSize != null) {
            list = jpaQuery.offset((pageNo - 1) * pageSize).limit(pageSize).fetch();
        } else {
            list = jpaQuery.fetch();
        }
        PageResult<ExchangeOrder> result = new PageResult<>(list, jpaQuery.fetchCount());
        result.setNumber(pageNo);
        result.setSize(pageSize);
        return result;
    }

    /**
     * 处理交易完成的订单
     * @param orderId 交易完成的订单
     * @param tradedAmount 成交量
     * @param turnover //成交额，对市价买单有用
     * @return
     */
    @Transactional
    public MessageResult tradeCompleted(String orderId, BigDecimal tradedAmount, BigDecimal turnover) {
        //根据订单id查询相应的交易订单
        ExchangeOrder order = exchangeOrderRepository.findByOrderId(orderId);
        //判断交易订单状态
        //如果订单不是交易状态(交易状态,完成状态,取消状态,超时状态),则返回无效订单
        if (order.getStatus() != ExchangeOrderStatus.TRADING) {
            return MessageResult.error(500, "invalid order(" + orderId + "),not trading status");
        }
        //成交量
        order.setTradedAmount(tradedAmount);
        //成交额，对市价买单有用
        order.setTurnover(turnover);
        //设置订单为完成状态
        order.setStatus(ExchangeOrderStatus.COMPLETED);
        //交易完成时间
        order.setCompletedTime(Calendar.getInstance().getTimeInMillis());
        //jpa的增加或修改
        exchangeOrderRepository.saveAndFlush(order);


        //处理用户钱包,对冻结作处理，剩余成交额退回
        orderRefund(order, tradedAmount, turnover);
        //查询一下是否是机器人订单
        RobotOrder robotOrder = robotOrderService.findByExchangeOrderId(order.getOrderId());
        //是得话,1为已关闭
        if (robotOrder != null) {
            robotOrder.setStatus(1);
            robotOrderService.save(robotOrder);
        } else {
            log.info("orderId" + order.getOrderId() + "订单不是机器人发布委托");
        }
        return MessageResult.success("tradeCompleted success");
    }

    /**
     * 委托退款，当1取消订单或2成交完成但有剩余时触发
     *
     * @param order
     * @param tradedAmount
     * @param turnover
     */
    public void orderRefund(ExchangeOrder order, BigDecimal tradedAmount, BigDecimal turnover) {
        //下单时候冻结的币，实际成交应扣的币
        //frozenBalance:冻结余额  dealBalance:交易余额
        BigDecimal frozenBalance, dealBalance;
        //判断订单方向为买入
        if (order.getDirection() == ExchangeOrderDirection.BUY) {
            //挂单类型为买入限价
            if (order.getType() == ExchangeOrderType.LIMIT_PRICE) {
                //冻结余额为买入量乘以挂单价格
                frozenBalance = order.getAmount().multiply(order.getPrice());
            } else {
                //挂单类型为买入市价
                //冻结余额为买入量
                frozenBalance = order.getAmount();
            }
            //交易额为成交额
            dealBalance = turnover;
        } else {
            //订单方向为卖出
            //冻结余额为卖出量
            frozenBalance = order.getAmount();
            //成交量
            dealBalance = tradedAmount;
        }
        //判断订单方向是买入还是卖出,如果是买入,则用结算单位 否则用币单位
        String coinSymbol = order.getDirection() == ExchangeOrderDirection.BUY ? order.getBaseSymbol() : order.getCoinSymbol();
        //根据换算单位和会员id查询出会员钱包
        MemberWallet wallet = memberWalletService.findByCoinUnitAndMemberId(coinSymbol, order.getMemberId());
        //返还的金额 = 冻结的金额 - 已成交的金额
        BigDecimal refundAmount = frozenBalance.subtract(dealBalance);
        //如果退币金额大于零的话查询一下是否是机器人订单,如果是的话则判断时间够不够三小时不够的话不允许退单是的话推送消息到kafka进行结束业务
        log.info("===cancel==退币：" + refundAmount);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            //查询一下是否是机器人订单
            RobotOrder robotOrder = robotOrderService.findByExchangeOrderId(order.getOrderId());
            if (robotOrder != null) {
//                Calendar cal = Calendar.getInstance();
//                cal.setTime(robotOrder.getCreateTime());
//                cal.add(Calendar.HOUR, 3);
//                log.info("当前时间" + new Date());
//                log.info("结束时间" + cal.getTime());
//                if (new Date().compareTo(cal.getTime()) >= 0) {
//                } else {
//                    memberWalletService.thawBalance(wallet, refundAmount);
//                }
            } else {
                //解冻钱包
                memberWalletService.thawBalance(wallet, refundAmount);
            }
        }
    }

    /**
     * 取消订单
     *
     * @param orderId 订单编号
     * @return
     */
    @Transactional
    public MessageResult cancelOrder(String orderId, BigDecimal tradedAmount, BigDecimal turnover) {
        ExchangeOrder order = findOne(orderId);
        if (order == null) {
            return MessageResult.error("order not exists");
        }
        if (order.getStatus() != ExchangeOrderStatus.TRADING) {
            return MessageResult.error(500, "order not in trading");
        }
        order.setTradedAmount(tradedAmount);
        order.setTurnover(turnover);
        order.setStatus(ExchangeOrderStatus.CANCELED);
        order.setCanceledTime(Calendar.getInstance().getTimeInMillis());
        //未成交的退款
        orderRefund(order, tradedAmount, turnover);
        return MessageResult.success();
    }


    /**
     * 获取某交易对当日已取消次数
     *
     * @param uid
     * @param symbol
     * @return
     */
    public long findTodayOrderCancelTimes(Long uid, String symbol) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.CANCELED, false));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTick = calendar.getTimeInMillis();
        calendar.add(Calendar.HOUR_OF_DAY, 24);
        long endTick = calendar.getTimeInMillis();
        specification.add(Restrictions.gte("canceledTime", startTick, false));
        specification.add(Restrictions.lt("canceledTime", endTick, false));
        return exchangeOrderRepository.count(specification);
    }

    /**
     * 查询当前正在交易的订单数量
     *
     * @param uid
     * @param symbol
     * @return
     */
    public long findCurrentTradingCount(Long uid, String symbol) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.count(specification);
    }

    public long findCurrentTradingCount(Long uid, String symbol, ExchangeOrderDirection direction) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("direction", direction, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.count(specification);
    }

    public List<ExchangeOrder> findOvertimeOrder(String symbol, int maxTradingTime) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        specification.add(Restrictions.eq("symbol", symbol, false));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -maxTradingTime);
        long tickTime = calendar.getTimeInMillis();
        specification.add(Restrictions.lt("time", tickTime, false));
        return exchangeOrderRepository.findAll(specification);
    }

    /**
     * 查询符合状态的订单
     *
     * @param cancelTime
     * @return
     */
    public List<ExchangeOrder> queryExchangeOrderByTime(long cancelTime) {
        return exchangeOrderRepository.queryExchangeOrderByTime(cancelTime);
    }

    public List<ExchangeOrder> queryExchangeOrderByTimeById(long cancelTime, long sellMemberId, long buyMemberId) {
        return exchangeOrderRepository.queryExchangeOrderByTimeById(cancelTime, sellMemberId, buyMemberId);
    }

    /**
     * API 添加订单接口
     *
     * @param memberId
     * @param order
     * @return
     */
    @Transactional
    public String addOrderForApi(Long memberId, ExchangeOrder order) {
        order.setTime(Calendar.getInstance().getTimeInMillis());
        order.setStatus(ExchangeOrderStatus.TRADING);
        order.setTradedAmount(BigDecimal.ZERO);
        order.setOrderId(GeneratorUtil.getOrderId("E"));
        log.info("add order:{}", order);
        if (order.getDirection() == ExchangeOrderDirection.BUY) {
            MemberWallet wallet = memberWalletService.findByCoinUnitAndMemberId(order.getBaseSymbol(), memberId);
            BigDecimal turnover;
            if (order.getType() == ExchangeOrderType.MARKET_PRICE) {
                turnover = order.getAmount();
            } else {
                turnover = order.getAmount().multiply(order.getPrice());
            }
            if (wallet.getBalance().compareTo(turnover) < 0) {
                return null;
            } else {
                memberWalletService.freezeBalance(wallet, turnover);
                //wallet.setBalance(wallet.getBalance().subtract(turnover));
                //wallet.setFrozenBalance(wallet.getFrozenBalance().add(turnover));
            }
        } else if (order.getDirection() == ExchangeOrderDirection.SELL) {
            MemberWallet wallet = memberWalletService.findByCoinUnitAndMemberId(order.getCoinSymbol(), memberId);
            if (wallet.getBalance().compareTo(order.getAmount()) < 0) {
                return null;
            } else {
                memberWalletService.freezeBalance(wallet, order.getAmount());
                //wallet.setBalance(wallet.getBalance().subtract(order.getAmount()));
                //wallet.setFrozenBalance(wallet.getFrozenBalance().add(order.getAmount()));
            }
        }
        order = exchangeOrderRepository.saveAndFlush(order);
        return order.getOrderId();
    }

    /**
     * Api 查询订单接口
     *
     * @param memberId
     * @param symbol
     * @param direction
     * @return
     */
    public Page<ExchangeOrder> findCurrentTradingOrderForApi(long memberId, String symbol, ExchangeOrderDirection direction, int pageNo, int pageSize) {
        Sort orders = new Sort(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = new PageRequest(pageNo, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("memberId", memberId, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        specification.add(Restrictions.eq("direction", direction, false));
        return exchangeOrderRepository.findAll(specification, pageRequest);
    }


    /**
     * 强制取消订单,在撮合中心和数据库订单不一致的情况下使用
     *
     * @param order
     */
    @Transactional
    public void forceCancelOrder(ExchangeOrder order) {
        List<ExchangeOrderDetail> details = exchangeOrderDetailService.findAllByOrderId(order.getOrderId());
        BigDecimal tradedAmount = BigDecimal.ZERO;
        BigDecimal turnover = BigDecimal.ZERO;
        for (ExchangeOrderDetail trade : details) {
            tradedAmount = tradedAmount.add(trade.getAmount());
            turnover = turnover.add(trade.getAmount().multiply(trade.getPrice()));
        }
        order.setTradedAmount(tradedAmount);
        order.setTurnover(turnover);
        if (order.isCompleted()) {
            tradeCompleted(order.getOrderId(), order.getTradedAmount(), order.getTurnover());
        } else {
            cancelOrder(order.getOrderId(), order.getTradedAmount(), order.getTurnover());
        }
    }
}

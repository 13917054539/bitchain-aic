package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.constant.CommonStatus;
import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.*;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.factory.ConstantFactory;
import cn.ztuo.bitrade.service.Base.TopBaseService;
import cn.ztuo.bitrade.service.CoinService;
import cn.ztuo.bitrade.service.LocaleMessageSourceService;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.service.MemberWalletService;
import cn.ztuo.bitrade.util.MessageResult;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SQLQuery;
import org.hibernate.Transaction;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Transformer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

/**
 * @author 十三
 * @date 2019年11月5日02:35:25
 */
@Slf4j
@Service
public class RobotOrderService extends MessageResult {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private RobotOrderDao robotOrderDao;

    @Autowired
    private MemberWalletDao memberWalletDao;

    @Autowired
    private MemberDao memberDao;
    @Autowired
    private SysConfigDao sysConfigDao;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MemberTransactionDao memberTransactionDao;

    @Transactional
    public MessageResult addSellOrder(RobotOrder robotOrder){
        log.info("Order+{}", robotOrder);
        if (robotOrder.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return error("投入金额有误");
        }
        Member member = memberDao.findMemberById(robotOrder.getMemberId());
        if (StringUtils.isEmpty(member.getAiosId())) {
            return error("请先绑定aios账号");
        }
        if (member.getStatus() == CommonStatus.ILLEGAL) {
            return error("账户已被封禁");
        }
        SellRobotOrder sellRobotOrder = new SellRobotOrder();
        sellRobotOrder.setMemberId(robotOrder.getMemberId());
        sellRobotOrder.setAmount(robotOrder.getAmount());
        sellRobotOrder.setBuyRate(new BigDecimal(0.0041));
        sellRobotOrder.setRate(BigDecimal.ZERO);
        sellRobotOrder.setCreateTime(new Date());
        sellRobotOrder.setStatus(0);
        sellRobotOrderDao.save(sellRobotOrder);
        return success("开启智能委托成功");
    }

    /**
     * 发布机器人交易单
     */
    public MessageResult addRobotOrder(RobotOrder robotOrder) {
        log.info("Order+{}", robotOrder);
        if (robotOrder.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return error("投入金额有误");
        }
        //根据会员id查找出会员信息
        Member member = memberDao.findMemberById(robotOrder.getMemberId());
        if (StringUtils.isEmpty(member.getAiosId())) {
            return error("请先绑定aios账号");
        }
        if (member.getStatus() == CommonStatus.ILLEGAL) {
            return error("账户已被封禁");
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //aios当前时价
        BigDecimal aiosRate = (BigDecimal) valueOperations.get(SysConstant.AIOS_RATE);
        if (aiosRate.compareTo(BigDecimal.ZERO) <= 0) {
            return error("时价为零禁止发布");
        }
        robotOrder.setAiosRate(aiosRate);//买入或者卖出的比率
        robotOrder.setBalance(robotOrder.getAmount());//剩余金额
        //设置状态为启动
        robotOrder.setStatus(0);//状态 0为启动中 ,1为已关闭 ,2
        if (robotOrder.getType() == 0) {//0为买 1为卖
            //固定涨幅
            BigDecimal increaseAmount = (BigDecimal) valueOperations.get(SysConstant.AMOUNT_OF_INCREASE);
            if(increaseAmount==null){
                increaseAmount=new BigDecimal(sysConfigDao.findOne(2L).getReValue());
                valueOperations.set(SysConstant.AMOUNT_OF_INCREASE,increaseAmount);
            }
//            BigDecimal increaseAmount = new BigDecimal("0.26");
            if (increaseAmount == null) {
                return error("暂停交易");
            } else {
                if (aiosRate.multiply(new BigDecimal("1.0041")).compareTo(increaseAmount) >= 0) {
                    BigDecimal rate;
                    do {
                        rate = new BigDecimal("0.9959").subtract(new BigDecimal("0.985")).multiply(new BigDecimal(Math.random())).add(new BigDecimal("0.985")).setScale(6, RoundingMode.DOWN);
                    } while (rate.compareTo(BigDecimal.ZERO) <= 0 && rate.compareTo(new BigDecimal("0.9959")) >= 0 );
                    aiosRate=increaseAmount.multiply(rate);
                    log.info("不符合正常币价修改当前币价为{},比率为{}",aiosRate,rate);
                } else {
                    log.info("符合正常币价正常发布买单");
                }
            }
            if (member.getAiosLimit().compareTo(BigDecimal.ZERO) > 0) {
                if ((robotOrder.getAmount().multiply(aiosRate).add(robotOrderDao.getByAmountSum(member.getId()))).compareTo(member.getAiosLimit()) > 0) {
                    return error("机器人挂单金额已上限");
                }
            } else {
                return error("机器人挂单金额为零");
            }
            ////投入金额单位默认为U
            robotOrder.setAmount(robotOrder.getAmount().divide(aiosRate, 2, BigDecimal.ROUND_DOWN));
            //买
            robotOrder.setPurchasePrice(aiosRate.multiply(new BigDecimal("1").add(robotOrder.getRate())));//发布价格

        } else if (robotOrder.getType() == 1) {
            //卖
            log.info("aiosRate:{},Rate:{}", aiosRate, robotOrder.getRate());
            robotOrder.setPurchasePrice(robotOrder.getBuyRate().multiply(new BigDecimal("1").add(robotOrder.getRate())));//发布价格
        }

        if (robotOrder.getType() == 0) {
            //发布限价买单

            //下一步
            MessageResult messageResult = addRobotOrder(ExchangeOrderDirection.BUY, "AIOS/USDT", robotOrder.getPurchasePrice(), robotOrder.getAmount(), ExchangeOrderType.LIMIT_PRICE, 0, member.getId(), robotOrder);
            if (messageResult.getCode() == 0) {
            } else {
                member.setRobotLimit(BigDecimal.ZERO);
                member.setRobotStatus(0);
                return messageResult;
            }
        } else if (robotOrder.getType() == 1) {
            //发布限价卖单
            MessageResult messageResult = addRobotOrder(ExchangeOrderDirection.SELL, "AIOS/USDT", robotOrder.getPurchasePrice(), robotOrder.getAmount(), ExchangeOrderType.LIMIT_PRICE, 0, member.getId(), robotOrder);
            if (messageResult.getCode() == 0) {
                log.info("机器人发送委托成功");
            } else {
                member.setRobotLimit(BigDecimal.ZERO);
                member.setRobotStatus(0);
                return messageResult;
            }
        } else {
            return error("非法订单");
        }
        log.info("发布成功");
        return success("发布成功");

    }

    /**
     * 委托
     * 0为当前委托 1为历史委托
     */
    public Map<String, Object> CommissionedByHistory(Long memberId, Integer status, Integer pageNo, Integer pageSize) {
        if (pageNo != null) {
            pageNo = (pageNo - 1) * pageSize;
            pageSize = pageSize;
        }
        StringBuffer sql = new StringBuffer();
        sql.append("select * from robot_order where member_id=:memberId and `status`=:status");
        sql.append(" order by create_time DESC");
        Query query = em.createNativeQuery(sql.toString(), RobotOrder.class);

        //设置结果转成Map类型
        query.setParameter("memberId", memberId);
        query.setParameter("status", status);
        int count = query.getResultList().size();
        int totalPages = count / pageSize;
        if (query.getResultList().size() % pageSize != 0) {
            totalPages++;
        }
        Member member = memberService.findOne(memberId);
        query.setFirstResult(pageNo);
        query.setMaxResults(pageSize);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalPages", totalPages);
        resultMap.put("count", count);
        resultMap.put("robotLimit", member.getRobotLimit());
        resultMap.put("status", member.getRobotStatus());
        resultMap.put("list", query.getResultList());
        return resultMap;
    }

    @Autowired
    private ExchangeOrderService orderService;
    @Autowired
    private MemberWalletService walletService;
    @Autowired
    private ExchangeCoinService exchangeCoinService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private int maxCancelTimes = -1;
    @Autowired
    private LocaleMessageSourceService msService;
    @Autowired
    private MemberService memberService;

    public RobotOrder findByExchangeOrderId(String exchangeOrderId) {
        return robotOrderDao.findByExchangeOrderId(exchangeOrderId);
    }

    public RobotOrder save(RobotOrder robotOrder) {
        return robotOrderDao.save(robotOrder);
    }

    public MessageResult addOrder(BigDecimal price, BigDecimal amount, ExchangeOrderDirection exchangeOrderDirection) {
        return addRobotOrder(exchangeOrderDirection, "AIOS/USDT", price, amount, ExchangeOrderType.LIMIT_PRICE, 0, 10000L);

    }

    private MessageResult addRobotOrder(
            ExchangeOrderDirection direction, String symbol, BigDecimal price,
            BigDecimal amount, ExchangeOrderType type, Integer isRobotOrder, Long memberId) {
       /* if(DateUtil.compareTime()){
            return MessageResult.error("交易停止中...");
        }*/
        int expireTime = SysConstant.USER_ADD_EXCHANGE_ORDER_TIME_LIMIT_EXPIRE_TIME;
        ValueOperations valueOperations = redisTemplate.opsForValue();
        if (direction == null || type == null) {
            return MessageResult.error(500, msService.getMessage("ILLEGAL_ARGUMENT"));
        }
        Member member = memberService.findOne(memberId);
//        if (member.getMemberLevel() == MemberLevelEnum.GENERAL) {
//            return MessageResult.error(500, "请先进行实名认证");
//        }
        //是否被禁止交易
        if (member.getTransactionStatus().equals(BooleanEnum.IS_FALSE)) {
            return MessageResult.error(500, msService.getMessage("CANNOT_TRADE"));
        }
        ExchangeOrder order = new ExchangeOrder();
        //判断限价输入值是否小于零
        if (price.compareTo(BigDecimal.ZERO) <= 0 && type == ExchangeOrderType.LIMIT_PRICE) {
            return MessageResult.error(500, msService.getMessage("EXORBITANT_PRICES"));
        }
        //判断数量小于零
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return MessageResult.error(500, msService.getMessage("NUMBER_OF_ILLEGAL"));
        }
        //根据交易对名称（symbol）获取交易对儿信息
        ExchangeCoin exchangeCoin = exchangeCoinService.findBySymbol(symbol);
        if (exchangeCoin == null || exchangeCoin.getEnable() != 1) {
            return MessageResult.error(500, msService.getMessage("NONSUPPORT_COIN"));
        }

        //获取基准币
        String baseCoin = exchangeCoin.getBaseSymbol();
        //获取交易币
        String exCoin = exchangeCoin.getCoinSymbol();
        Coin coin;
        //根据交易方向查询币种信息
        if (direction == ExchangeOrderDirection.SELL) {
            coin = coinService.findByUnit(exCoin);
        } else {
            coin = coinService.findByUnit(baseCoin);
        }
        if (coin == null) {
            return MessageResult.error(500, msService.getMessage("NONSUPPORT_COIN"));
        }
        //设置价格精度
        price = price.setScale(exchangeCoin.getBaseCoinScale(), BigDecimal.ROUND_DOWN);
        //委托数量和精度控制
        if (direction == ExchangeOrderDirection.BUY && type == ExchangeOrderType.MARKET_PRICE) {
            amount = amount.setScale(exchangeCoin.getBaseCoinScale(), BigDecimal.ROUND_DOWN);
            //最小成交额控制
            if (amount.compareTo(exchangeCoin.getMinTurnover()) < 0) {
                return MessageResult.error(500, "成交额至少为" + exchangeCoin.getMinTurnover());
            }
        } else {
            amount = amount.setScale(exchangeCoin.getCoinScale(), BigDecimal.ROUND_DOWN);
            //成交量范围控制
            if (exchangeCoin.getMaxVolume() != null && exchangeCoin.getMaxVolume().compareTo(BigDecimal.ZERO) != 0
                    && exchangeCoin.getMaxVolume().compareTo(amount) < 0) {
                return MessageResult.error(msService.getMessage("AMOUNT_OVER_SIZE") + " " + exchangeCoin.getMaxVolume());
            }
            if (exchangeCoin.getMinVolume() != null && exchangeCoin.getMinVolume().compareTo(BigDecimal.ZERO) != 0
                    && exchangeCoin.getMinVolume().compareTo(amount) > 0) {
                return MessageResult.error(msService.getMessage("AMOUNT_TOO_SMALL") + " " + exchangeCoin.getMinVolume());
            }
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0 && type == ExchangeOrderType.LIMIT_PRICE) {
            return MessageResult.error(500, msService.getMessage("EXORBITANT_PRICES"));
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return MessageResult.error(500, msService.getMessage("NUMBER_OF_ILLEGAL"));
        }
        MemberWallet baseCoinWallet = walletService.findByCoinUnitAndMemberId(baseCoin, member.getId());
        MemberWallet exCoinWallet = walletService.findByCoinUnitAndMemberId(exCoin, member.getId());
        if (baseCoinWallet == null || exCoinWallet == null) {
            return MessageResult.error(500, msService.getMessage("NONSUPPORT_COIN"));
        }
        if (baseCoinWallet.getIsLock() == BooleanEnum.IS_TRUE || exCoinWallet.getIsLock() == BooleanEnum.IS_TRUE) {
            return MessageResult.error(500, msService.getMessage("WALLET_LOCKED"));
        }
        //如果有最低卖价限制，出价不能低于此价,且禁止市场价格卖
        if (direction == ExchangeOrderDirection.SELL && exchangeCoin.getMinSellPrice().compareTo(BigDecimal.ZERO) > 0
                && ((price.compareTo(exchangeCoin.getMinSellPrice()) < 0) || type == ExchangeOrderType.MARKET_PRICE)) {
            return MessageResult.error(500, msService.getMessage("EXORBITANT_PRICES"));
        }
        //查看是否启用市价买卖
        if (type == ExchangeOrderType.MARKET_PRICE) {
            if (exchangeCoin.getEnableMarketBuy() == BooleanEnum.IS_FALSE && direction == ExchangeOrderDirection.BUY) {
                return MessageResult.error(500, "不支持市价购买");
            } else if (exchangeCoin.getEnableMarketSell() == BooleanEnum.IS_FALSE && direction == ExchangeOrderDirection.SELL) {
                return MessageResult.error(500, "不支持市价出售");
            }
        }
        //限制委托数量
        if (exchangeCoin.getMaxTradingOrder() > 0 && orderService.findCurrentTradingCount(member.getId(), symbol, direction) >= exchangeCoin.getMaxTradingOrder()) {
            return MessageResult.error(500, "超过最大挂单数量 " + exchangeCoin.getMaxTradingOrder());
        }
        order.setMemberId(member.getId());
        order.setSymbol(symbol);
        order.setBaseSymbol(baseCoin);
        order.setCoinSymbol(exCoin);
        order.setType(type);
        order.setDirection(direction);
        if (order.getType() == ExchangeOrderType.MARKET_PRICE) {
            order.setPrice(BigDecimal.ZERO);
        } else {
            order.setPrice(price);
        }
        order.setUseDiscount("0");
        //限价买入单时amount为用户设置的总成交额
        order.setAmount(amount);
        log.info("======================" + order);
        MessageResult mr = orderService.addOrder(member.getId(), order);
        if (mr.getCode() != 0) {
            return MessageResult.error(500, "提交订单失败:" + mr.getMessage());
        }
        log.info(">>>>>>>>>>订单提交完成>>>>>>>>>>");
        // 发送消息至Exchange系统
        kafkaTemplate.send("exchange-order", JSON.toJSONString(order));
        MessageResult result = MessageResult.success("success");
        result.setData(order.getOrderId());
        return result;
    }


    private MessageResult addRobotOrder(
            ExchangeOrderDirection direction, String symbol, BigDecimal price,
            BigDecimal amount, ExchangeOrderType type, Integer isRobotOrder, Long memberId, RobotOrder robotOrder) {
       /* if(DateUtil.compareTime()){
            return MessageResult.error("交易停止中...");
        }*/
        int expireTime = SysConstant.USER_ADD_EXCHANGE_ORDER_TIME_LIMIT_EXPIRE_TIME;
        ValueOperations valueOperations = redisTemplate.opsForValue();
        if (direction == null || type == null) {
            return MessageResult.error(500, msService.getMessage("ILLEGAL_ARGUMENT"));
        }
        Member member = memberService.findOne(memberId);
//        if (member.getMemberLevel() == MemberLevelEnum.GENERAL) {
//            return MessageResult.error(500, "请先进行实名认证");
//        }
        //是否被禁止交易
        if (member.getTransactionStatus().equals(BooleanEnum.IS_FALSE)) {
            return MessageResult.error(500, msService.getMessage("CANNOT_TRADE"));
        }
        ExchangeOrder order = new ExchangeOrder();
        //判断限价输入值是否小于零
        if (price.compareTo(BigDecimal.ZERO) <= 0 && type == ExchangeOrderType.LIMIT_PRICE) {
            return MessageResult.error(500, msService.getMessage("EXORBITANT_PRICES"));
        }
        //判断数量小于零
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return MessageResult.error(500, msService.getMessage("NUMBER_OF_ILLEGAL"));
        }
        //根据交易对名称（symbol）获取交易对儿信息
        ExchangeCoin exchangeCoin = exchangeCoinService.findBySymbol(symbol);
        if (exchangeCoin == null || exchangeCoin.getEnable() != 1) {
            return MessageResult.error(500, msService.getMessage("NONSUPPORT_COIN"));
        }

        //获取基准币
        String baseCoin = exchangeCoin.getBaseSymbol();
        //获取交易币
        String exCoin = exchangeCoin.getCoinSymbol();
        Coin coin;
        //根据交易方向查询币种信息
        if (direction == ExchangeOrderDirection.SELL) {
            coin = coinService.findByUnit(exCoin);
        } else {
            coin = coinService.findByUnit(baseCoin);
        }
        if (coin == null) {
            return MessageResult.error(500, msService.getMessage("NONSUPPORT_COIN"));
        }
        //设置价格精度
        price = price.setScale(exchangeCoin.getBaseCoinScale(), BigDecimal.ROUND_DOWN);
        //委托数量和精度控制
        if (direction == ExchangeOrderDirection.BUY && type == ExchangeOrderType.MARKET_PRICE) {
            amount = amount.setScale(exchangeCoin.getBaseCoinScale(), BigDecimal.ROUND_DOWN);
            //最小成交额控制
            if (amount.compareTo(exchangeCoin.getMinTurnover()) < 0) {
                return MessageResult.error(500, "成交额至少为" + exchangeCoin.getMinTurnover());
            }
        } else {
            amount = amount.setScale(exchangeCoin.getCoinScale(), BigDecimal.ROUND_DOWN);
            //成交量范围控制
            if (exchangeCoin.getMaxVolume() != null && exchangeCoin.getMaxVolume().compareTo(BigDecimal.ZERO) != 0
                    && exchangeCoin.getMaxVolume().compareTo(amount) < 0) {
                return MessageResult.error(msService.getMessage("AMOUNT_OVER_SIZE") + " " + exchangeCoin.getMaxVolume());
            }
            if (exchangeCoin.getMinVolume() != null && exchangeCoin.getMinVolume().compareTo(BigDecimal.ZERO) != 0
                    && exchangeCoin.getMinVolume().compareTo(amount) > 0) {
                return MessageResult.error(msService.getMessage("AMOUNT_TOO_SMALL") + " " + exchangeCoin.getMinVolume());
            }
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0 && type == ExchangeOrderType.LIMIT_PRICE) {
            return MessageResult.error(500, msService.getMessage("EXORBITANT_PRICES"));
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return MessageResult.error(500, msService.getMessage("NUMBER_OF_ILLEGAL"));
        }
        MemberWallet baseCoinWallet = walletService.findByCoinUnitAndMemberId(baseCoin, member.getId());
        MemberWallet exCoinWallet = walletService.findByCoinUnitAndMemberId(exCoin, member.getId());
        if (baseCoinWallet == null || exCoinWallet == null) {
            return MessageResult.error(500, msService.getMessage("NONSUPPORT_COIN"));
        }
        if (baseCoinWallet.getIsLock() == BooleanEnum.IS_TRUE || exCoinWallet.getIsLock() == BooleanEnum.IS_TRUE) {
            return MessageResult.error(500, msService.getMessage("WALLET_LOCKED"));
        }
        //如果有最低卖价限制，出价不能低于此价,且禁止市场价格卖
        if (direction == ExchangeOrderDirection.SELL && exchangeCoin.getMinSellPrice().compareTo(BigDecimal.ZERO) > 0
                && ((price.compareTo(exchangeCoin.getMinSellPrice()) < 0) || type == ExchangeOrderType.MARKET_PRICE)) {
            return MessageResult.error(500, msService.getMessage("EXORBITANT_PRICES"));
        }
        //查看是否启用市价买卖
        if (type == ExchangeOrderType.MARKET_PRICE) {
            if (exchangeCoin.getEnableMarketBuy() == BooleanEnum.IS_FALSE && direction == ExchangeOrderDirection.BUY) {
                return MessageResult.error(500, "不支持市价购买");
            } else if (exchangeCoin.getEnableMarketSell() == BooleanEnum.IS_FALSE && direction == ExchangeOrderDirection.SELL) {
                return MessageResult.error(500, "不支持市价出售");
            }
        }
        //限制委托数量
        if (exchangeCoin.getMaxTradingOrder() > 0 && orderService.findCurrentTradingCount(member.getId(), symbol, direction) >= exchangeCoin.getMaxTradingOrder()) {
            return MessageResult.error(500, "超过最大挂单数量 " + exchangeCoin.getMaxTradingOrder());
        }
        order.setMemberId(member.getId());
        order.setSymbol(symbol);
        order.setBaseSymbol(baseCoin);
        order.setCoinSymbol(exCoin);
        order.setType(type);
        order.setDirection(direction);
        if (order.getType() == ExchangeOrderType.MARKET_PRICE) {
            order.setPrice(BigDecimal.ZERO);
        } else {
            order.setPrice(price);
        }
        order.setUseDiscount("0");//是否使用折扣 0 不使用 1使用
        //限价买入单时amount为用户设置的总成交额
        order.setAmount(amount);
        log.info("======================" + order);
        MessageResult mr = orderService.addOrder(member.getId(), order);
        if (mr.getCode() != 0) {
            return MessageResult.error(500, "提交订单失败:" + mr.getMessage());
        }
        log.info(">>>>>>>>>>订单提交完成>>>>>>>>>>");
        // 发送消息至Exchange系统
        kafkaTemplate.send("exchange-order", JSON.toJSONString(order));
        MessageResult result = MessageResult.success("success");
        result.setData(order.getOrderId());
        robotOrder.setExchangeOrderId(order.getOrderId());
        robotOrder.setCreateTime(new Date());
        robotOrderDao.save(robotOrder);
        return result;
    }

    public MessageResult cancelOrder(Long memberId, String orderId) {
        if (StringUtils.isEmpty(orderId)) {
            return error("订单号为空");
        }
        ExchangeOrder order = orderService.findOne(orderId);
        if (order == null) {
            return error("撮合器中无此订单");
        }
        if (!order.getMemberId().equals(memberId)) {
            return MessageResult.error(500, "禁止操作");
        }
        if (order.getStatus() != ExchangeOrderStatus.TRADING) {
            return MessageResult.error(500, "订单不在交易");
        }
        if (getExchangeOrder(order) != null) {
            if (maxCancelTimes > 0 && orderService.findTodayOrderCancelTimes(memberId, order.getSymbol()) >= maxCancelTimes) {
                return MessageResult.error(500, "你今天已经取消了 " + maxCancelTimes + " 次");
            }
            ExchangeOrder exchangeOrder = getExchangeOrder(order);
            RobotOrder robotOrder = findByExchangeOrderId(exchangeOrder.getOrderId());
            if (robotOrder != null) {
//                Calendar cal = Calendar.getInstance();
//                cal.setTime(robotOrder.getCreateTime());
//                cal.add(Calendar.HOUR, 3);
//                if (new Date().compareTo(cal.getTime()) >= 0 ||robotOrder.getType()==0) {
                kafkaTemplate.send("exchange-order-cancel-robot", JSON.toJSONString(order));
//                } else {
//                    log.info("定时器不处理未过期订单");
//                }
            } else {
                // 发送消息至Exchange系统
                kafkaTemplate.send("exchange-order-cancel", JSON.toJSONString(order));
            }
        } else {
            //强制取消
            orderService.forceCancelOrder(order);
        }
        return MessageResult.success("success");
    }

    /**
     * 查找撮合交易器中订单是否存在
     *
     * @param order
     * @return
     */
    public ExchangeOrder getExchangeOrder(ExchangeOrder order) {
        try {
            String serviceName = "SERVICE-EXCHANGE-TRADE";
            String url = "http://" + serviceName + "/monitor/order?symbol=" + order.getSymbol() + "&orderId=" + order.getOrderId() + "&direction=" + order.getDirection() + "&type=" + order.getType();
            ResponseEntity<ExchangeOrder> result = restTemplate.getForEntity(url, ExchangeOrder.class);
            return result.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<RobotOrder> getRobotByStatusAnAndCreateTime() {
        return robotOrderDao.getRobotByStatusAnAndCreateTime();
    }

    public List<RobotOrder> getRobotByStatus() {
        return robotOrderDao.getRobotByStatus();
    }

    public List<RobotOrder> getReleaseOrder() {
        return robotOrderDao.getReleaseOrder();
    }

    public BigDecimal getTodayAmount(Long memberId) {
        return robotOrderDao.getTodayAmount(memberId);
    }

    public BigDecimal getTodayAmountSum() {
        return robotOrderDao.getTodayAmountSum();
    }

    /**
     * 获取所有委托
     */
    /**
     * 委托
     * 0为当前委托 1为历史委托
     */
    public MessageResult getRobotOrderPage(Long memberId, Integer type, Integer pageNo, Integer pageSize) {
        if (pageNo != null) {
            pageNo = (pageNo - 1) * pageSize;
            pageSize = pageSize;
        } else {
            pageNo = 0;
            pageSize = 10;
        }
        StringBuffer sql = new StringBuffer();
        sql.append("select * from robot_order where 1=1 ");
        if (!StringUtils.isEmpty(memberId)) {
            sql.append(" and member_id=:memberId ");
        }
        if (!StringUtils.isEmpty(type)) {
            sql.append(" and type=:type ");
        }
        sql.append(" order by create_time ");
        Query query = em.createNativeQuery(sql.toString(), RobotOrder.class);

        //设置结果转成Map类型
        if (!StringUtils.isEmpty(memberId)) {
            query.setParameter("memberId", memberId);
        }
        if (!StringUtils.isEmpty(type)) {
            query.setParameter("type", type);
        }
        int count = query.getResultList().size();
        int totalPages = count / pageSize;
        if (query.getResultList().size() % pageSize != 0) {
            totalPages++;
        }
        query.setFirstResult(pageNo);
        query.setMaxResults(pageSize);
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put("totalPages", totalPages);
        resultMap.put("count", count);
        resultMap.put("list", query.getResultList());
        resultMap.put("historicalGrossTransactionVolume", robotOrderDao.getHistoricalGrossTransactionVolume());
        resultMap.put("totalTurnoverToday", robotOrderDao.getTotalTurnoverToday());
        resultMap.put("sellOrderSumToday", robotOrderDao.getOrderSumToday(1));
        resultMap.put("byOrderSumToday", robotOrderDao.getOrderSumToday(0));
        return getSuccessInstance("获取成功", resultMap);
    }

    /**
     * 获取卖单延长时间
     */
    public MessageResult getRobotTime() {
        return getSuccessInstance("获取成功", constantFactory.getRegulationValueById(1L));
    } /**
     * 获取卖单延长时间
     */
    public MessageResult getBAmount() {
        return getSuccessInstance("获取成功", constantFactory.getRegulationValueById(2L));
    }

    @Autowired
    private ConstantFactory constantFactory;

    /**
     * 修改卖单延长时间
     */
    public MessageResult updateBAmount(String value) {
        SysConfig sysConfig = new SysConfig();
        if (new BigDecimal(value).compareTo(new BigDecimal("0")) < 0) {
            return error("最低币价不允许低于0");
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        sysConfig.setId(2L);
        sysConfig.setReValue(value);
        sysConfigDao.save(sysConfig);
        BigDecimal increaseAmount=new BigDecimal(sysConfigDao.findOne(2L).getReValue());
        valueOperations.set(SysConstant.AMOUNT_OF_INCREASE,increaseAmount);
        return getSuccessInstance("修改成功", sysConfig);
    }
    /**
     * 修改卖单延长时间
     */
    public MessageResult updateRobotTime(String value) {
        SysConfig sysConfig = new SysConfig();
        if (new BigDecimal(value).compareTo(new BigDecimal("10")) < 0) {
            return error("最小时长不允许低于十分钟");
        }
        sysConfig.setId(1L);
        sysConfig.setReValue(value);
        sysConfigDao.save(sysConfig);
        return getSuccessInstance("修改成功", sysConfig);
    }

    /**
     * 获取当个用户的买单
     */
    public List<RobotOrder> getRobotOrderByMemberId(Long memberId) {
        StringBuffer sql = new StringBuffer();
        sql.append("select * from robot_order where member_id=:memberId and status =0 ");
        Query query = em.createNativeQuery(sql.toString(), RobotOrder.class);
        query.setParameter("memberId", memberId);
        return query.getResultList();
    }

    @Autowired
    private SellRobotOrderDao sellRobotOrderDao;

    /**
     * 关闭用户的预订单
     */
    public void closeSellRobotOrder(Long memberId) {
        List<SellRobotOrder> sellRobotOrders = sellRobotOrderDao.findByMemberIdAndStatus(memberId, 0);
        sellRobotOrders.forEach(sellRobotOrder -> {
            sellRobotOrder.setStatus(2);
        });
        sellRobotOrderDao.save(sellRobotOrders);
    }

}


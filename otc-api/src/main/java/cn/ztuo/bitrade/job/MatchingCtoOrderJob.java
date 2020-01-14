package cn.ztuo.bitrade.job;

import cn.ztuo.bitrade.coin.CoinExchangeFactory;
import cn.ztuo.bitrade.constant.*;
import cn.ztuo.bitrade.dao.AdvertiseDao;
import cn.ztuo.bitrade.dao.MemberDao;
import cn.ztuo.bitrade.entity.Advertise;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.Order;
import cn.ztuo.bitrade.entity.OtcCoin;
import cn.ztuo.bitrade.entity.chat.ChatMessageRecord;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.exception.InformationExpiredException;
import cn.ztuo.bitrade.service.AdvertiseService;
import cn.ztuo.bitrade.service.LocaleMessageSourceService;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.service.OrderService;
import cn.ztuo.bitrade.util.DateUtil;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static cn.ztuo.bitrade.constant.PayMode.ALI;
import static cn.ztuo.bitrade.constant.PayMode.BANK;
import static cn.ztuo.bitrade.constant.PayMode.WECHAT;
import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;
import static cn.ztuo.bitrade.util.BigDecimalUtils.*;
import static cn.ztuo.bitrade.util.BigDecimalUtils.compare;
import static cn.ztuo.bitrade.util.BigDecimalUtils.sub;
import static org.springframework.util.Assert.isTrue;

/**
 * @Auther:路道
 * @Date:2019/9/9
 * @Description:cn.ztuo.bitrade.job
 * @version:1.0
 */
@Slf4j
@Component
public class MatchingCtoOrderJob {

    @Autowired
    private CoinExchangeFactory coins;
    @Autowired
    private AdvertiseDao advertiseDao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private AdvertiseService advertiseService;
    @Autowired
    private LocaleMessageSourceService msService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private OrderService orderService;

    @Autowired
    private MongoTemplate mongoTemplate ;

    @Value("${spark.system.order.sms:1}")
    private int notice;

    /**
     * 功能描述: 匹配CTO交易订单
     * @param:
     * @return:0 0/30 * * * ?
     */
    @Scheduled(cron ="0 0/30 * * * ?" )
    public void  matchingCtoOrder(){
        try {
            ctoOrder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 功能描述: 匹配CTO订单
     * @param:
     * @return:
     */
    public void ctoOrder(){
        //查询上架中的买家信息
        List<Advertise> buyAdvertises = advertiseDao.allCtoAdvertise(0);
        if(StringUtils.isEmpty(buyAdvertises) || buyAdvertises.size()<1){
            return;
        }
        //查询上架中的卖家信息
        List<Advertise> sellAdvertises = advertiseDao.allCtoAdvertise(1);
        if(StringUtils.isEmpty(sellAdvertises) || sellAdvertises.size()<1){
            return;
        }
        //批量处理
        for (Advertise bAdvertise:buyAdvertises){
            //查询卖家信息
            Advertise advertises = advertiseDao.allShellCtoAdvertise(bAdvertise.getNumber());

        }

        /*//如果没得匹配了就结束
            if(sellAdvertises.size()==0){
                return;
            }
            //买家待匹配数量
            BigDecimal bremainAmount = bAdvertise.getRemainAmount();
            if(bremainAmount.compareTo(BigDecimal.ZERO)<1){
                continue;
            }
            //买家已匹配数量
            BigDecimal matchedNum=BigDecimal.ZERO;

            for (Advertise sAdvertise:sellAdvertises){
                //如果买家待匹配数量小于卖家待匹配数量,则该笔交易已处理完毕
                if(bremainAmount.compareTo(sAdvertise.getRemainAmount())<1){

                }else {

                }
            }*/

        //优先匹配最先上架买家和卖家（匹配完才会匹配下一单）

        //匹配成功之后扣除卖家剩余数量

        /*price：单价
        max_limit:单笔最多成交量
        min_limit:单笔最低成交量
        number:买入/卖出总量
        remain_amount:买入/卖出剩余总量
        deal_amount:买入/卖出 dealAmount 交易中数量*/


    }

    /**
     * 买币
     *
     * @param id
     * @param coinId
     * @param price
     * @param money
     * @param amount
     * @param remark
     * @param user
     * @return
     * @throws InformationExpiredException
     */
    public void buy(long id, long coinId, BigDecimal price, BigDecimal money,
                    BigDecimal amount, String remark,
                    @RequestParam(value = "mode", defaultValue = "0") Integer mode,
                    @SessionAttribute(SESSION_MEMBER) AuthMember user) throws InformationExpiredException {
        Advertise advertise = advertiseService.findOne(id);
        if (advertise == null || !advertise.getAdvertiseType().equals(AdvertiseType.SELL)) {
            log.info(msService.getMessage("PARAMETER_ERROR"));
            return;
        }
        isTrue(!user.getName().equals(advertise.getMember().getUsername()), msService.getMessage("NOT_ALLOW_BUY_BY_SELF"));
        isTrue(advertise.getStatus().equals(AdvertiseControlStatus.PUT_ON_SHELVES), msService.getMessage("ALREADY_PUT_OFF"));
        OtcCoin otcCoin = advertise.getCoin();
        if (otcCoin.getId() != coinId) {
            log.info(msService.getMessage("PARAMETER_ERROR"));
            return;
        }
        if (advertise.getPriceType().equals(PriceType.REGULAR)) {
            isTrue(isEqual(price, advertise.getPrice()), msService.getMessage("PRICE_EXPIRED"));
        } else {
            BigDecimal marketPrice = coins.get(otcCoin.getUnit());
            isTrue(isEqual(price, mulRound(rate(advertise.getPremiseRate()), marketPrice, 2)), msService.getMessage("PRICE_EXPIRED"));
        }
        if (mode == 0) {
            isTrue(isEqual(div(money, price), amount), msService.getMessage("NUMBER_ERROR"));
        } else {
            isTrue(isEqual(mulRound(amount, price,2), money), msService.getMessage("NUMBER_ERROR"));
        }
        isTrue(compare(money, advertise.getMinLimit()), msService.getMessage("MONEY_MIN") + advertise.getMinLimit().toString() + " CNY");
        isTrue(compare(advertise.getMaxLimit(), money), msService.getMessage("MONEY_MAX") + advertise.getMaxLimit().toString() + " CNY");
        String[] pay = advertise.getPayMode().split(",");
        //计算手续费
        //if(advertise.getMember().getCertifiedBusinessStatus()==)
        BigDecimal commission = mulRound(amount, getRate(otcCoin.getJyRate()));

        //认证商家法币交易免手续费
        Member member = memberService.findOne(user.getId());
        log.info("会员等级************************************:{},********,{}",member.getCertifiedBusinessStatus(),member.getMemberLevel());
        if(member.getCertifiedBusinessStatus().equals(CertifiedBusinessStatus.VERIFIED)
                && member.getMemberLevel().equals(MemberLevelEnum.IDENTIFICATION)) {
            commission = BigDecimal.ZERO ;
        }
        isTrue(compare(advertise.getRemainAmount(), amount), msService.getMessage("AMOUNT_NOT_ENOUGH"));
        Order order = new Order();
        order.setStatus(OrderStatus.NONPAYMENT);
        order.setAdvertiseId(advertise.getId());
        order.setAdvertiseType(advertise.getAdvertiseType());
        order.setCoin(otcCoin);
        order.setCommission(commission);
        order.setCountry(advertise.getCountry().getZhName());
        order.setCustomerId(user.getId());
        order.setCustomerName(user.getName());
        order.setCustomerRealName(member.getRealName());
        order.setMemberId(advertise.getMember().getId());
        order.setMemberName(advertise.getMember().getUsername());
        order.setMemberRealName(advertise.getMember().getRealName());
        order.setMaxLimit(advertise.getMaxLimit());
        order.setMinLimit(advertise.getMinLimit());
        order.setMoney(money);
        order.setNumber(sub(amount,commission));
        order.setPayMode(advertise.getPayMode());
        order.setPrice(price);
        order.setRemark(remark);
        order.setTimeLimit(advertise.getTimeLimit());
        Arrays.stream(pay).forEach(x -> {
            if (ALI.getCnName().equals(x)) {
                order.setAlipay(advertise.getMember().getAlipay());
            } else if (WECHAT.getCnName().equals(x)) {
                order.setWechatPay(advertise.getMember().getWechatPay());
            } else if (BANK.getCnName().equals(x)) {
                order.setBankInfo(advertise.getMember().getBankInfo());
            }
        });
        if (!advertiseService.updateAdvertiseAmountForBuy(advertise.getId(), amount)) {
            throw new InformationExpiredException("Information Expired");
        }
        Order order1 = orderService.saveOrder(order);
        if (order1 != null) {
            if (notice==1){
                try {
                    //smsProvider.sendMessageByTempId(advertise.getMember().getMobilePhone(), advertise.getCoin().getUnit()+"##"+user.getName(),"9499");
                } catch (Exception e) {
                    log.error("sms 发送失败");
                    e.printStackTrace();
                }
            }
            /**
             * 下单后，将自动回复记录添加到mongodb
             */
            if(advertise.getAuto()==BooleanEnum.IS_TRUE){
                ChatMessageRecord chatMessageRecord = new ChatMessageRecord();
                chatMessageRecord.setOrderId(order1.getOrderSn());
                chatMessageRecord.setUidFrom(order1.getMemberId().toString());
                chatMessageRecord.setUidTo(order1.getCustomerId().toString());
                chatMessageRecord.setNameFrom(order1.getMemberName());
                chatMessageRecord.setNameTo(order1.getCustomerName());
                chatMessageRecord.setContent(advertise.getAutoword());
                chatMessageRecord.setSendTime(Calendar.getInstance().getTimeInMillis());
                chatMessageRecord.setSendTimeStr(DateUtil.getDateTime());
                //自动回复消息保存到mogondb
                mongoTemplate.insert(chatMessageRecord,"chat_message");
            }
            MessageResult result = MessageResult.success(msService.getMessage("CREATE_ORDER_SUCCESS"));
            result.setData(order1.getOrderSn().toString());
            return ;
        } else {
            throw new InformationExpiredException("Information Expired");
        }
    }


    public static void main(String[] args) {
        List<String> list=new ArrayList<>();
        list.add("asd");
        list.add("ZXczx");
        System.out.println(list.get(0));
    }

}

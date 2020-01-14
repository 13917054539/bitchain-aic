package cn.ztuo.bitrade.controller;


import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.alibaba.fastjson.JSONObject;
import com.sparkframework.lang.Convert;

import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.entity.MemberWallet;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.es.ESUtils;
import cn.ztuo.bitrade.service.MemberTransactionService;
import cn.ztuo.bitrade.service.MemberWalletService;
import cn.ztuo.bitrade.system.CoinExchangeFactory;
import cn.ztuo.bitrade.util.DateUtil;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/asset")
@Slf4j
public class AssetController {
    @Autowired
    private MemberWalletService walletService;
    @Autowired
    private MemberTransactionService transactionService;
    @Autowired
    private CoinExchangeFactory coinExchangeFactory;
    @Value("${gcx.match.max-limit:1000}")
    private double gcxMatchMaxLimit;
    @Value("${gcx.match.each-limit:5}")
    private double gcxMatchEachLimit;
    @Autowired
    private KafkaTemplate kafkaTemplate;
    @Autowired
    private ESUtils esUtils;

    /**
     * 用户钱包信息
     *
     * @param member
     * @return
     */
    @RequestMapping("wallet")
    public MessageResult findWallet(@SessionAttribute(SESSION_MEMBER) AuthMember member) {
        Sort sort=new Sort(Sort.Direction.ASC,"coin.sort");
        List<MemberWallet> wallets = walletService.findAllByMemberIdOrderByVersionDesc(member.getId(),sort);
        wallets.forEach(wallet -> {
            CoinExchangeFactory.ExchangeRate rate = coinExchangeFactory.get(wallet.getCoin().getUnit());
            if (rate != null) {
                log.info("Unit:{},UsdRate:{},CnyRate():{}",wallet.getCoin().getUnit(),rate.getUsdRate().doubleValue(),rate.getCnyRate().doubleValue());
                wallet.getCoin().setUsdRate(rate.getUsdRate().doubleValue());
                wallet.getCoin().setCnyRate(rate.getCnyRate().doubleValue());
            } else {
                log.info("unit = {} , rate = null ", wallet.getCoin().getUnit());
            }
        });
        MessageResult mr = MessageResult.success("success");
        mr.setData(wallets);
        return mr;
    }

    /**
     * 查询特定类型的记录
     *
     * @param member
     * @param pageNo
     * @param pageSize
     * @param type
     * @return
     */
    @RequestMapping("transaction")
    public MessageResult findTransaction(@SessionAttribute(SESSION_MEMBER) AuthMember member, int pageNo, int pageSize, TransactionType type) {
        MessageResult mr = new MessageResult();
        mr.setData(transactionService.queryByMember(member.getId(), pageNo, pageSize, type));
        mr.setCode(0);
        mr.setMessage("success");
        return mr;
    }

    /**
     * 查询所有记录
     *
     * @param member
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping("transaction/all")
    public MessageResult findTransaction(@SessionAttribute(SESSION_MEMBER) AuthMember member, HttpServletRequest request, Integer pageNo, Integer pageSize,
                                         @RequestParam(value = "startTime",required = false)  String startTime,
                                         @RequestParam(value = "endTime",required = false)  String endTime,
                                         @RequestParam(value = "symbol",required = false)  String symbol,
                                         @RequestParam(value = "type",required = false)  String type) throws ParseException {
        MessageResult mr = new MessageResult();
        TransactionType transactionType = null;
        if (StringUtils.isNotEmpty(type)) {
            transactionType = TransactionType.valueOfOrdinal(Convert.strToInt(type, 0));
        }
        mr.setCode(0);
        mr.setMessage("success");
        mr.setData(transactionService.queryByMember(member.getId(), pageNo, pageSize, transactionType, startTime, endTime,symbol));
        return mr;
    }

    @RequestMapping("wallet/{symbol}")
    public MessageResult findWalletBySymbol(@SessionAttribute(SESSION_MEMBER) AuthMember member, @PathVariable String symbol) {
        MessageResult mr = MessageResult.success("success");
        mr.setData(walletService.findByCoinUnitAndMemberId(symbol, member.getId()));
        return mr;
    }

    @RequestMapping("wallet/reset-address")
    public MessageResult resetWalletAddress(@SessionAttribute(SESSION_MEMBER) AuthMember member, String unit) {
        try {
            MemberWallet memberWallet = walletService.findByCoinUnitAndMemberId(unit, member.getId());
            if(memberWallet==null||StringUtils.isEmpty(memberWallet.getAddress())){
                JSONObject json = new JSONObject();
                json.put("uid", member.getId());
                kafkaTemplate.send("reset-member-address", unit, json.toJSONString());
            }
            return MessageResult.success("提交成功");
        } catch (Exception e) {
            return MessageResult.error("未知异常");
        }
    }

    @RequestMapping("transaction_es")
    public MessageResult findTransactionByES(@RequestParam(value = "memberId",required = true) long memberId,
                                             @RequestParam(value = "page",required = true) int pageNum,
                                             @RequestParam(value = "limit",required = true)  int pageSize,
                                             @RequestParam(value = "startTime",required = false)  String startTime,
                                             @RequestParam(value = "endTime",required = false)  String endTime,
                                             @RequestParam(value = "symbol",required = false)  String symbol,
                                             @RequestParam(value = "type",required = false)  String type)  {
        log.info(">>>>>>查询交易明细开始>>>>>>>>>");
        MessageResult messageResult = new MessageResult();
        try {
            String query="{\"from\":"+(pageNum-1)*pageSize+",\"size\":"+ pageSize+",\"sort\":[{\"create_time\":{\"order\":\"desc\"}}]," +
                    "\"query\":{\"bool\":{\"must\":[{\"match\":{\"member_id\":\""+memberId+"\"}}";

            if(StringUtils.isNotEmpty(symbol)){
                query=query+",{\"match\":{\"symbol\":\""+symbol+"\"}}";
            }
            if(StringUtils.isNotEmpty(type)){
                query=query+",{\"match\":{\"type\":\""+type+"\"}}";
            }
            if(StringUtils.isNotEmpty(startTime)&&StringUtils.isNotEmpty(endTime)){
                query =query+",{\"range\":{\"create_time\":{\"gte\":\""+startTime+"\",\"lte\":\""+endTime+"\"}}}";
            }
            query=query+"]}}}";
            JSONObject resultJson = esUtils.queryForAnyOne(JSONObject.parseObject(query),"member_transaction","mem_transaction");
            messageResult.setCode(0);
            messageResult.setData(resultJson);
            messageResult.setMessage("success");
        } catch (Exception e) {
            log.info(">>>>>>查询es错误>>>>>>"+e);
            e.printStackTrace();
            return MessageResult.error(500,"查询异常，请稍后再试");
        }
        return messageResult;
    }

}

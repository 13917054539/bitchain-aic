package cn.ztuo.bitrade.job;

import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.service.*;
import cn.ztuo.bitrade.util.AESUtil;
import cn.ztuo.bitrade.util.HttpUtils;
import cn.ztuo.bitrade.util.MessageResult;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.List;

@Slf4j
@Component
public class AicJob {

    @Autowired
    private RobotOrderService robotOrderService;



    @Autowired
    private ExchangeOrderService exchangeOrderService;
    /**
     * key
     */
    private final static String KEY = "AyHjstBNFHgVnE4n";


    /**
     * 每分钟查询币币交易订单更新机器人状态 * 0-59 * * * ?
     */
//    @Scheduled(cron = "* 0-59 * * * ? ")
    @Scheduled(fixedDelay=1000*10)
    public void scanComspletedOrder() {
        List<RobotOrder> robotOrders = robotOrderService.getRobotByStatus();
        log.info("扫描到" + robotOrders.size() + "笔过期订单开始处理");
        robotOrders.forEach(robotOrder -> {
            synchronized (robotOrder) {
                ExchangeOrder exchangeOrder = exchangeOrderService.findOne(robotOrder.getExchangeOrderId());
                if (exchangeOrder != null) {
                    if (exchangeOrder.getStatus() == ExchangeOrderStatus.CANCELED) {//取消
                        log.info(robotOrder.getId() + "已取消");
                        if (exchangeOrder.getTradedAmount().compareTo(BigDecimal.ZERO) == 0) { //
                            robotOrder.setStatus(2);
                        }else{
                            robotOrder.setStatus(1);
                        }
                    } else if (exchangeOrder.getStatus() == ExchangeOrderStatus.COMPLETED) {//完成
                        log.info(robotOrder.getId() + "完成");
                        robotOrder.setStatus(1);
                    }
                    robotOrderService.save(robotOrder);
                    log.info(robotOrder.getId() + "更新订单状态");
                }
            }
        });
    }

    /**
     * 每10分钟更新三小时后作废订单
     */
//    @Scheduled(cron = "0/5 * * * * ?")
//    @Scheduled(fixedDelay=1000*60)
//    public void expiredOrders() {
//        List<RobotOrder> robotOrders = robotOrderService.getRobotByStatusAnAndCreateTime();
//        log.info("扫描到" + robotOrders.size() + "笔作废订单开始处理");
//        robotOrders.forEach(robotOrder -> {
//            try {
//                MessageResult messageResult = robotOrderService.cancelOrder(robotOrder.getMemberId(), robotOrder.getExchangeOrderId());
//                if (messageResult.getCode() == 0) {
//                    log.info("结束订单成功");
//                } else {
//                    log.info("结束订单失败");
//                    log.info(messageResult.getMessage());
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//    }

    @Autowired
    private MemberService memberService;
    @Autowired
    private ExchangeCoinService exchangeCoinService;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 释放订单
     */
    @Scheduled(fixedDelay=1000*10)
    public void ReleaseOrder() {
        List<RobotOrder> robotOrders = robotOrderService.getReleaseOrder();
        log.info("扫描到" + robotOrders.size() + "笔待释放订单开始处理");
        ExchangeCoin exchangeCoin = exchangeCoinService.findBySymbol("AIOS/USDT");
        JSONObject jsonObject = new JSONObject();
        robotOrders.forEach(robotOrder -> {
            synchronized (robotOrder) {
                Member member = memberService.findOne(robotOrder.getMemberId());
                log.info("用户" + member.getMobilePhone() + "开始释放订单");
                jsonObject.put("id", member.getAiosId());
                jsonObject.put("exchangeId", member.getId());
                BigDecimal capital = robotOrder.getBalance();//.multiply(robotOrder.getAiosRate())
                jsonObject.put("transactionAmount", capital);//流水
                jsonObject.put("feeAmount", capital.multiply(exchangeCoin.getFee()));//手续费
                log.info(jsonObject.toJSONString());
                String msg = null;
                try {
                    msg = AESUtil.Encrypt(jsonObject.toJSONString(), KEY);
                    String encoder = URLEncoder.encode(msg, "UTF-8");
                    String result = HttpUtils.sendPost("http://www.aios-asc.com/qistoken/api/award/tradingDig", "info=" + encoder);
//                    HttpResponse<String> response = Unirest.post("http://www.aios-asc.com/qistoken/api/award/tradingDig").field("info",encoder).asString();
                    if(result==null){
                        log.info("请求内盘服务器发生异常");
                        return;
                    }
                    JSONObject object = JSONObject.parseObject(result);
                    if (object.getInteger("code") == 0) {
                        robotOrder.setIsRelease(1);
                        robotOrder.setRemark(object.getString("message"));
                        log.info(robotOrder.getId() + "释放成功");
                    } else {
                        robotOrder.setIsRelease(3);
                        robotOrder.setRemark(object.getString("message"));
                        log.info(member.getId()+"释放失败,订单号是"+robotOrder.getId());
                    }
                    robotOrderService.save(robotOrder);
                } catch (Exception e) {
                    log.info("请求内盘服务器发生异常");
                    log.info(member.getId()+"释放失败,订单号是"+robotOrder.getId());
                    e.printStackTrace();
                }
            }
        });
    }


    public static void main(String[] args) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", "10005");
        jsonObject.put("exchangeId", "10000");
        jsonObject.put("feeAmount", new BigDecimal("1000000"));
        jsonObject.put("transactionAmount", new BigDecimal("1000000"));
        String msg = AESUtil.Encrypt(jsonObject.toJSONString(), KEY);
        System.err.println(msg);
        String encoder = URLEncoder.encode(msg, "UTF-8");
//        String result = HttpUtils.sendPost("http://www.aios-asc.com/qistoken/api/award/tradingDig", "info=" + encoder);
        HttpResponse<String> response = Unirest.post("http://www.aios-asc.com/qistoken/api/award/tradingDig").field("info",encoder).asString();
        if(response==null){
            log.info("请求内盘服务器发生异常");
            return;
        }
        JSONObject object = JSONObject.parseObject(response.getBody());
        System.out.println(object);
        if (object.getInteger("code") == 0) {
            log.info( "释放成功");
        } else {
            log.info("释放失败,订单号是");
        }
        System.out.println(response);

    }
}
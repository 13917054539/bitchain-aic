package cn.ztuo.bitrade.job;

import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.system.CoinExchangeFactory;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.util.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class CheckExchangeRate {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CoinExchangeFactory factory;
    private String serviceName = "bitrade-market";

    @Scheduled(fixedRate = 60 * 1000)
    public void syncRate() {
        BigDecimal cnyRate = getUsdCnyRate();
        factory.getCoins().forEach((symbol, value) -> {
            BigDecimal usdRate = getUsdRate(symbol);
            factory.set(symbol, usdRate, cnyRate.multiply(usdRate).setScale(2, RoundingMode.UP));
        });
    }

    @Scheduled(fixedRate = 6 * 1000)
    public void syncAiosRate() {
        BigDecimal usdRate = getUsdRate("AIOS");
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set(SysConstant.AIOS_RATE, usdRate);
    }

    private BigDecimal getUsdRate(String unit) {
        String url = "http://" + serviceName + "/market/exchange-rate/usd/{coin}";
        ResponseEntity<MessageResult> result = restTemplate.getForEntity(url, MessageResult.class, unit);
        if (result.getStatusCode().value() == 200 && result.getBody().getCode() == 0) {
            BigDecimal rate = new BigDecimal((String) result.getBody().getData());
            return rate;
        } else {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getUsdCnyRate() {
        String url = "http://" + serviceName + "/market/exchange-rate/usd-cny";
        ResponseEntity<MessageResult> result = restTemplate.getForEntity(url, MessageResult.class);
        log.info("remote call:url={},result={}", url, result);
        if (result.getStatusCode().value() == 200 && result.getBody().getCode() == 0) {
            BigDecimal rate = new BigDecimal((Double) result.getBody().getData());
            return rate;
        } else {
            return BigDecimal.ZERO;
        }
    }
}

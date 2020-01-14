package cn.ztuo.bitrade.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import cn.ztuo.bitrade.Trader.CoinTrader;
import cn.ztuo.bitrade.Trader.CoinTraderFactory;
import cn.ztuo.bitrade.entity.ExchangeCoin;
import cn.ztuo.bitrade.service.ExchangeCoinService;
import cn.ztuo.bitrade.service.ExchangeOrderService;

import java.util.List;

@Slf4j
@Configuration
public class CoinTraderConfig {

    /**
     * 配置交易处理类
     * @param exchangeCoinService
     * @param kafkaTemplate
     * @return
     */
    @Bean
    public CoinTraderFactory getCoinTrader(ExchangeCoinService exchangeCoinService, KafkaTemplate<String,String> kafkaTemplate, ExchangeOrderService exchangeOrderService){
        //交易币的
        CoinTraderFactory factory = new CoinTraderFactory();
        //从数据库查找所有可用得交易币
        List<ExchangeCoin> coins = exchangeCoinService.findAllEnabled();
        for(ExchangeCoin coin:coins) {
            log.info("init trader,symbol={}",coin.getSymbol());
            //交易币
            CoinTrader trader = new CoinTrader(coin.getSymbol());
            //设置kafka
            trader.setKafkaTemplate(kafkaTemplate);
            //基币小数精度
            trader.setBaseCoinScale(coin.getBaseCoinScale());
            //交易币小数精度
            trader.setCoinScale(coin.getCoinScale());
           // 停止交易，取消当前所有订单
            trader.stopTrading();
            factory.addTrader(coin.getSymbol(),trader);
        }
        return factory;
    }

}

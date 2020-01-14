package cn.ztuo.bitrade.job;

import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.handler.MarketHandler;
import cn.ztuo.bitrade.handler.MongoMarketHandler;
import cn.ztuo.bitrade.handler.NettyHandler;
import cn.ztuo.bitrade.processor.CoinProcessor;
import cn.ztuo.bitrade.processor.CoinProcessorFactory;
import cn.ztuo.bitrade.service.ExchangeCoinService;
import cn.ztuo.bitrade.service.HuoBiTickersService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import javax.annotation.Resource;
import java.math.RoundingMode;
import java.util.*;

/**
 * 获取最新的行情推送
 */
@Component
@Slf4j
public class HuoBiTickersGeneratorJob {


    @Value("${platform.token}")
    private String platformToken;
    /**
     * 所有交易对的最新 Tickers
     * */
    private String MARKET_TICKERS = "http://api.huobi.pro/market/tickers";
    @Autowired
    private HuoBiTickersService huoBiTickersService;
    @Autowired
    private ExchangeCoinService coinService;
    @Autowired
    private CoinProcessorFactory coinProcessorFactory;
    @Autowired
    private MongoMarketHandler marketHandler;
    @Autowired
    private ExchangePushJob pushJob;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NettyHandler nettyHandler;

    /**
     * 10秒获取最新的行情
     * */
    @Scheduled(fixedRate = 3000)
    public void generatorNewsTicker(){
        List<ExchangeCoin> coins = coinService.findAllEnabled();
        List<String> coinArr = new ArrayList<>();
        for (ExchangeCoin exchangeCoin:coins) {
            if(exchangeCoin.getSymbol().contains("CAL")){
                continue;
            }
            coinArr.add(exchangeCoin.getSymbol().replace("/","").toLowerCase());
        }
        try {
            HttpResponse<JsonNode> resp = Unirest.get(MARKET_TICKERS)
                    .asJson();
            JSONObject jsonObject = JSON.parseObject(resp.getBody().toString());
            JSONArray resultArray = jsonObject.getJSONArray("data");
            if(resultArray==null){
                return;
            }
            int count = 0;
            for(int i=0;i<resultArray.size();i++){
                JSONObject result = resultArray.getJSONObject(i);
                String symbol = result.getString("symbol");
                for (ExchangeCoin exchangeCoin:coins) {
                    if(exchangeCoin.getSymbol().replace("/","").toLowerCase().equalsIgnoreCase(symbol)){
                        count++;
                        CoinProcessor processor = coinProcessorFactory.getProcessor(exchangeCoin.getSymbol());
                        CoinThumb thumb = processor.getThumb();
                        if(thumb==null){
                            continue;
                        }
                        thumb.setVolume(result.getBigDecimal("amount").setScale(4, RoundingMode.DOWN));
                        thumb.setOpen(result.getBigDecimal("open").setScale(4, RoundingMode.DOWN));
                        thumb.setLow(result.getBigDecimal("low").setScale(4, RoundingMode.DOWN));
                        thumb.setHigh(result.getBigDecimal("high").setScale(4, RoundingMode.DOWN));
                        thumb.setClose(result.getBigDecimal("close").setScale(4, RoundingMode.DOWN));
                        thumb.setChange(thumb.getClose().subtract(thumb.getOpen()));
                        thumb.setChg(thumb.getChange().divide(thumb.getOpen(), 4, RoundingMode.UP));
                        processor.setThumb(thumb);
                        //pushJob.addThumb(exchangeCoin.getSymbol(),thumb);
                    }
                }
               /* if (coinArr.contains(symbol)) {
                    count++;
                    HuoBiTickers huoBiTickers = new HuoBiTickers();
                    huoBiTickers.setSymbol(symbol);
                    huoBiTickers.setAmount(result.getBigDecimal("amount"));
                    huoBiTickers.setClose(result.getBigDecimal("close"));
                    huoBiTickers.setCount(result.getBigDecimal("count"));
                    huoBiTickers.setHigh(result.getBigDecimal("high"));
                    huoBiTickers.setLow(result.getBigDecimal("low"));
                    huoBiTickers.setVol(result.getBigDecimal("vol"));
                    huoBiTickersService.handleTickers(huoBiTickers);
                }*/
                if(count>=coinArr.size()){
                    break;
                }
            }
        } catch (RestClientException e) {
            e.printStackTrace();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取交易信息
     * */
    @Scheduled(fixedRate = 1000)
    public void pushThumb(){
        List<ExchangeCoin> coins = coinService.findAllEnabled();
        for(ExchangeCoin coin:coins){
            if(coin.getSymbol().contains(platformToken)||coin.getSymbol().contains("CTO")){
                continue;
            }
            String symbol2 =  coin.getSymbol().replace("/","").toLowerCase();
            String url = "http://api.huobi.pro/market/trade?symbol="+symbol2;
            HttpResponse<JsonNode> resp = null;
            try {
                resp = Unirest.get(url)
                        .asJson();
            } catch (UnirestException e) {
                e.printStackTrace();
            }
            JSONObject jsonReult = JSON.parseObject(resp.getBody().toString());
            JSONObject jsonObject2 = jsonReult.getJSONObject("tick");
            if(jsonObject2==null){
                return ;
            }
            JSONArray jsonArray = jsonObject2.getJSONArray("data");
            if(jsonArray==null){
                return ;
            }
            for(int i=0;i<jsonArray.size();i++){
                JSONObject data = jsonArray.getJSONObject(i);
                ExchangeTrade exchangeTrade = new ExchangeTrade();
                exchangeTrade.setAmount(data.getBigDecimal("amount").setScale(3,RoundingMode.DOWN));
                exchangeTrade.setPrice(data.getBigDecimal("price"));
                if (data.getString("direction").equals("sell")) {
                    exchangeTrade.setDirection(ExchangeOrderDirection.SELL);
                }else{
                    exchangeTrade.setDirection(ExchangeOrderDirection.BUY);
                }
                exchangeTrade.setTime(data.getLong("ts"));
                CoinProcessor processor = coinProcessorFactory.getProcessor(coin.getSymbol());
                nettyHandler.handleTrade(coin.getSymbol(),exchangeTrade,processor.getThumb());
                marketHandler.handleTrade(coin.getSymbol(),exchangeTrade,null);
            }
        }
    }

    @Scheduled(fixedRate = 6000)
    public void pushTradePlate(){
        List<ExchangeCoin> coins = coinService.findAllEnabled();
        for(ExchangeCoin coin:coins){
            if(coin.getSymbol().contains(platformToken)||coin.getSymbol().contains("CTO")){
                continue;
            }
            String symbol2 =  coin.getSymbol().replace("/","").toLowerCase();
            String url = "http://api.huobi.pro/market/depth?symbol="+symbol2+"&type=step0";
            HttpResponse<JsonNode> resp = null;
            try {
                resp = Unirest.get(url)
                        .asJson();
            } catch (UnirestException e) {
                e.printStackTrace();
            }
            TradePlate buyTradePlate =  new TradePlate();
            buyTradePlate.setDirection(ExchangeOrderDirection.BUY);
            buyTradePlate.setSymbol(coin.getSymbol());
            TradePlate sellTradePlate =  new TradePlate();
            sellTradePlate.setDirection(ExchangeOrderDirection.SELL);
            sellTradePlate.setSymbol(coin.getSymbol());
            JSONObject jsonReult = JSON.parseObject(resp.getBody().toString());
            JSONObject jsonObject = jsonReult.getJSONObject("tick");
            LinkedList<TradePlateItem> items = new LinkedList<>();
            LinkedList<TradePlateItem> items2 = new LinkedList<>();
            if(jsonObject!=null){
                JSONArray bidsJsonArray=jsonObject.getJSONArray("bids");
                for(int i=0;i<20;i++){
                    TradePlateItem tradePlateItem = new TradePlateItem();
                    JSONArray bids = bidsJsonArray.getJSONArray(i);
                    tradePlateItem.setPrice(bids.getBigDecimal(0));
                    tradePlateItem.setAmount(bids.getBigDecimal(1));
                    items.add(tradePlateItem);
                }
                JSONArray asksJsonArray = jsonObject.getJSONArray("asks");
                for(int i=0;i<20;i++){
                    JSONArray asks = asksJsonArray.getJSONArray(i);
                    TradePlateItem tradePlateItem = new TradePlateItem();
                    tradePlateItem.setPrice(asks.getBigDecimal(0).setScale(3, RoundingMode.DOWN));
                    tradePlateItem.setAmount(asks.getBigDecimal(1));
                    items2.add(tradePlateItem);
                }
            }
            buyTradePlate.setItems(items2);
            sellTradePlate.setItems(items);
            nettyHandler.handlePlate(coin.getSymbol(),buyTradePlate);
            nettyHandler.handlePlate(coin.getSymbol(),sellTradePlate);
            //pushJob.addPlates(coin.getSymbol(),buyTradePlate);
            //pushJob.addPlates(coin.getSymbol(),sellTradePlate);
        }
    }



    /*@Scheduled(fixedRate = 2000)
    public void pushThumb(){
        List<ExchangeCoin> coins = coinService.findAllEnabled();
        for(ExchangeCoin coin:coins){
            CoinProcessor processor = coinProcessorFactory.getProcessor(coin.getSymbol());
            CoinThumb thumb = processor.getThumb();
            if(!coin.getSymbol().contains("ANT")){
                if(thumb==null){
                    continue;
                }
                String symbol2 = coin.getSymbol().replace("/","").toLowerCase();
                List<HuoBiTickers> huoBiTickers = huoBiTickersService.tickers();
                for (HuoBiTickers huoBiTicker:huoBiTickers) {
                    if(huoBiTicker.getSymbol().equalsIgnoreCase(symbol2)){
                        thumb.setVolume(huoBiTicker.getAmount().setScale(4, RoundingMode.DOWN));
                        thumb.setOpen(huoBiTicker.getOpen().setScale(4, RoundingMode.DOWN));
                        thumb.setLow(huoBiTicker.getLow().setScale(4, RoundingMode.DOWN));
                        thumb.setHigh(huoBiTicker.getHigh().setScale(4, RoundingMode.DOWN));
                        thumb.setClose(huoBiTicker.getClose().setScale(4, RoundingMode.DOWN));
                        break;
                    }
                }
                synchronized (thumb) {
                    messagingTemplate.convertAndSend("/topic/market/thumb",thumb);
                }
            }
        }
    }
*/

}

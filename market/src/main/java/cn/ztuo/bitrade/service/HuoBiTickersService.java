package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.component.CoinExchangeRate;
import cn.ztuo.bitrade.entity.CoinThumb;
import cn.ztuo.bitrade.entity.ExchangeCoin;
import cn.ztuo.bitrade.entity.HuoBiTickers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author: eiven
 * @date: 2019/6/11 19:07
 */
@Service
public class HuoBiTickersService {
    @Autowired
    private MongoTemplate mongoTemplate;
    /**
     * 获取最新聚合行情
     * */
    private String DETAIL_MERGED = "https://api.huobi.pro/market/detail/merged?symbol=";



    /**
     * 存储行情
     * @param huoBiTickers
     */
    public void handleTickers(HuoBiTickers huoBiTickers) {
        Query query = new Query();
        query.addCriteria(Criteria.where("symbol").is(huoBiTickers.getSymbol()));
        Update update = new Update();
        update.set("open", huoBiTickers.getOpen());
        update.set("close", huoBiTickers.getClose());
        update.set("low", huoBiTickers.getLow());
        update.set("high", huoBiTickers.getHigh());
        update.set("amount", huoBiTickers.getAmount());
        update.set("count", huoBiTickers.getCount());
        update.set("vol", huoBiTickers.getVol());
        mongoTemplate.upsert(query, update, HuoBiTickers.class);
    }

    /**
     * 行情列表数据
     */
    public List<HuoBiTickers> tickers() {
      return  mongoTemplate.findAll(HuoBiTickers.class);
    }


}

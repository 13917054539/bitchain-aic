package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.coin.CoinExchangeFactory;
import cn.ztuo.bitrade.service.OtcCoinService;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.ztuo.bitrade.util.MessageResult.success;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author GS
 * @date 2018年01月06日
 */
@RestController
@Slf4j
@RequestMapping(value = "/coin")
public class OtcCoinController {

    @Autowired
    private OtcCoinService coinService;
    @Autowired
    private CoinExchangeFactory coins;

    /**
     * 取得正常的币种
     *
     * @return
     */
    @RequestMapping(value = "all")
    public MessageResult allCoin() throws Exception {
        //获取CTO内盘价信息
        BigDecimal bigDecimal = coinService.ctoPrice();
        List<Map<String, String>> list = coinService.getAllNormalCoin();
        list.stream().forEachOrdered(x ->{
            if(coins.get(x.get("unit")) != null && !x.get("unit").equals("CTO")) {
                x.put("marketPrice", coins.get(x.get("unit")).toString());
            }else {
                x.put("marketPrice", bigDecimal.toString());
            }

        });
        MessageResult result = success();
        result.setData(list);
        return result;
    }
}

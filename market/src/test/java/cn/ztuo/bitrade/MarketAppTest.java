package cn.ztuo.bitrade;
import cn.ztuo.bitrade.entity.ExchangeTrade;
import cn.ztuo.bitrade.service.ReleaseService;
import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import cn.ztuo.bitrade.MarketApplication;
import cn.ztuo.bitrade.service.ExchangeOrderService;
import cn.ztuo.bitrade.service.MarketService;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MarketApplication.class)
public class MarketAppTest {
    @Autowired
    private MarketService marketService;
    @Autowired
    private ExchangeOrderService exchangeOrderService;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    ReleaseService releaseService;



    @Test
    public void contextLoads() throws Exception {
        releaseService.release();
    }

}

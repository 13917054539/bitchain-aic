package cn.ztuo.bitrade;
import cn.ztuo.bitrade.component.CoinExchangeRate;
import cn.ztuo.bitrade.dao.MemberDao;
import cn.ztuo.bitrade.dao.PatternConfDao;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.PatternConf;
import cn.ztuo.bitrade.service.ContractService;
import cn.ztuo.bitrade.service.ReleaseService;
import cn.ztuo.bitrade.service.RobotOrderService;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import cn.ztuo.bitrade.MarketApplication;
import cn.ztuo.bitrade.dao.OrderDetailAggregationRepository;
import cn.ztuo.bitrade.entity.OrderDetailAggregation;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 * @author GS
 * @date 2018年03月22日
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes=MarketApplication.class)
public class FeeTest {
    @Autowired
    private OrderDetailAggregationRepository orderDetailAggregationRepository;
    @Autowired
    private RobotOrderService robotOrderService;

    @Test
    public void contextLoads() throws Exception {
        System.err.println("=================================================================");
        System.out.println(robotOrderService.getRobotOrderByMemberId(13727L));
        System.err.println("=================================================================");

    }
    @Autowired
    private ReleaseService releaseService;

    @Autowired
    private PatternConfDao patternConfDao;

    @Autowired
    private CoinExchangeRate coinExchangeRate;

    @Autowired
    private ContractService contractService;
    @Autowired
    private MemberDao memberDao;


    @Test
    public void contextLoads1(){
        releaseService.release();
    }
}

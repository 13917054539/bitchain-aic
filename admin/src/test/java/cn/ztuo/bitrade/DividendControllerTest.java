package cn.ztuo.bitrade;

import cn.ztuo.bitrade.constant.ActivityRewardType;
import cn.ztuo.bitrade.dao.*;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.service.*;
import cn.ztuo.bitrade.util.DateUtil;

import cn.ztuo.bitrade.util.MessageResult;
import com.querydsl.core.types.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.springframework.util.Assert.notNull;

/**
 * @author GS
 * @date 2018年03月22日
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes=WebApplication.class)
public class DividendControllerTest {
    @Autowired
    private OrderDetailAggregationService orderDetailAggregationService;
    @Autowired
    SellRobotOrderDao sellRobotOrderDao;


    @Test
    public void queryStatistics(){
        RobotOrder robotOrder =new RobotOrder();
        robotOrder.setSellRobotOrderId(13237L);
        SellRobotOrder sellRobotOrder=sellRobotOrderDao.getOrderById(robotOrder.getSellRobotOrderId());
        System.err.println(sellRobotOrder);
    }

    @Autowired
    private ExchangeReleaseTokenService exchangeReleaseTokenService;

    @Test
    public void queryStatistics2(){

        List<ExchangeReleaseToken> exchangeReleaseTokens=   exchangeReleaseTokenService.findByReleaseTimeBetweenAndMemberIdIs(DateUtil.getNowStartTime().getTime(),
                DateUtil.getNowEndTime().getTime(),11);
        System.out.println(exchangeReleaseTokens);
        ExchangeReleaseToken exchangeReleaseToken = new ExchangeReleaseToken();
        exchangeReleaseToken.setMemberId(11);
        exchangeReleaseToken.setOrderId(11111+"");
        exchangeReleaseToken.setReleaseAmount(new BigDecimal("123124"));
        exchangeReleaseToken.setReleaseTime(System.currentTimeMillis());
        exchangeReleaseTokenService.save(exchangeReleaseToken);
        //System.out.println(exchangeOrderRepository.selectBuyOrderCountByCompleteTime(DateUtil.getNowStartTime().getTime(),DateUtil.getNowEndTime().getTime(),11));
    }


    @Autowired
    private MemberDao memberDao;

    @Autowired
    private MemberBorrowingReturningDao memberBorrowingReturningDao;

    @Autowired
    private RewardPromotionSettingDao rewardPromotionSettingDao;

    @Autowired
    private MemberWalletService memberWalletService;

    @Autowired
    private MemberWalletDao memberWalletDao;

    @Autowired
    private MemberTransactionDao memberTransactionDao;
    @Autowired
    private BorrowingAndReturningService borrowingAndReturningService;

    @Autowired
    private MemberTransactionService memberTransactionService;
    @Autowired
    private  MemberTransactionDao transactionDao;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    RewardActivitySettingService rewardActivitySettingService;
    @Autowired
    private MemberApplicationService memberApplicationService;

    @Test
    public void  test(){

        borrowingAndReturningService.borrowingAndReturning(540,0,"3000","123456");

    }

    @Autowired
    private PatternConfDao patternConfDao;

    @Test
    public void contextLoads1(){
        patternConfDao.getPatternConf();
        System.out.println("wqeqwe");
        //releaseService.release();
    }


}

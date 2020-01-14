package cn.ztuo.bitrade;

import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.AefPatternConfDao;
import cn.ztuo.bitrade.dao.MemberBorrowingReturningDao;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.MemberWalletDao;
import cn.ztuo.bitrade.entity.AefPatternConf;
import cn.ztuo.bitrade.entity.MemberTransaction;
import cn.ztuo.bitrade.entity.MemberWallet;
import cn.ztuo.bitrade.service.AefPatternService;
import cn.ztuo.bitrade.service.ContractService;
import cn.ztuo.bitrade.service.MemberTransactionService;
import cn.ztuo.bitrade.system.CoinExchangeFactory;
import cn.ztuo.bitrade.util.FunctionUtils;
import cn.ztuo.bitrade.util.MessageResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * @author GS
 * @date 2018年02月06日
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = UcenterApplication.class)
public class BasicApplicationTest {

    @Autowired
    private MemberBorrowingReturningDao memberBorrowingReturningDao;

    @Autowired
    private CoinExchangeFactory coinExchangeFactory;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private MemberWalletDao memberWalletDao;

    @Autowired
    private MemberTransactionService memberTransactionService;
    @Autowired
    private MemberTransactionDao memberTransactionDao;
    @Autowired
    private AefPatternConfDao aefPatternConfDao;

    private String serviceName = "bitrade-market";
    @Autowired
    private AefPatternService aefPatternService;
    @Autowired
    private ContractService contractService;

    @Test
    public void test(){
        contractService.addOrRelieve(1153L,"USDT","500",2);
    }


}

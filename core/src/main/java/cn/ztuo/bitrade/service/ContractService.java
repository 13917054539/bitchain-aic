package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.AefPatternConfDao;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.MemberWalletDao;
import cn.ztuo.bitrade.entity.AefPatternConf;
import cn.ztuo.bitrade.entity.Coin;
import cn.ztuo.bitrade.entity.MemberTransaction;
import cn.ztuo.bitrade.entity.MemberWallet;
import cn.ztuo.bitrade.util.FunctionUtils;
import cn.ztuo.bitrade.util.MessageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * @Auther:路道
 * @Date:2019/7/6
 * @Description:cn.ztuo.bitrade.service
 * @version:1.0
 */
@Service
public class ContractService {

    @Autowired
    private MemberWalletDao memberWalletDao;
    @Autowired
    private MemberTransactionService memberTransactionService;
    @Autowired
    private MemberTransactionDao memberTransactionDao;
    @Autowired
    private AefPatternConfDao aefPatternConfDao;
    @Autowired
    private RestTemplate restTemplate;

    private String serviceName = "bitrade-market";

    /**
     * 功能描述: 加入或解除合约
     * @param:
     * @return:
     */
    @Transactional
    public synchronized MessageResult addOrRelieve(long id, String coin, String num, Integer type) {
        if(coin.equals("CMB")){
            return MessageResult.error("暂不支持该币种");
        }
        Coin co=new Coin();
        co.setName(coin);
        //获取用户钱包信息
        MemberWallet memberWallet = memberWalletDao.findByCoinAndMemberId(co, id);
        if(type==1){
            if(StringUtils.isEmpty(num)){
                return MessageResult.error("请输入合约数量");
            }
            if(new BigDecimal(num).compareTo(BigDecimal.ZERO)!=1){
                return MessageResult.error("输入合约数量有误");
            }

            //查看加入合约配置
            AefPatternConf aefPatternConf = aefPatternConfDao.getAefPatternConf(1);
           /* if(new BigDecimal(num).compareTo(aefPatternConf.getMinnum())==-1){
                return MessageResult.error("加入合约数量不能小于"+aefPatternConf.getMinnum()+"USDT");
            }*/
            //根据币种信息获取对应的转换USDT的汇率
            /*String url = "http://" + serviceName + "/market/exchange-rate/usd/{coin}";
            ResponseEntity<MessageResult> resultUsdt = restTemplate.getForEntity(url, MessageResult.class, co.getName());
            BigDecimal rateUsdt=BigDecimal.ZERO;
            if (resultUsdt.getStatusCode().value() == 200 && resultUsdt.getBody().getCode() == 0) {
                rateUsdt = new BigDecimal((String) resultUsdt.getBody().getData());
            }
            //算出可转换USDT的数量
            BigDecimal usdtNum = FunctionUtils.mul(new BigDecimal("num"), rateUsdt, 4);

            if(usdtNum.compareTo(aefPatternConf.getMaxnum())==1){
                return MessageResult.error("加入合约数量不能大于"+aefPatternConf.getMaxnum()+"USDT");
            }*/
            if(memberWallet.getBalance().compareTo(new BigDecimal(num))<=0){
                return MessageResult.error("可用余额不足");
            }
            //减少用户钱包余额
            int i = memberWalletDao.decreaseBalance(memberWallet.getId(), new BigDecimal(num));
            if(i==0){
                return MessageResult.error("加入合约失败");
            }
            insertDetile( new BigDecimal(num).negate(),memberWallet.getMemberId(),memberWallet.getCoin().getName(),TransactionType.JOINING_THE_CONTRACT);
            //增加释放余额
            memberWalletDao.increaseToReleased(memberWallet.getId(), new BigDecimal(num));
            insertDetile( new BigDecimal(num),memberWallet.getMemberId(),memberWallet.getCoin().getName(),TransactionType.JOINING_THE_CONTRACT);
            return MessageResult.success("加入成功");
        }else if(type==2){
            //减少钱包释放余额
            int i = memberWalletDao.decreaseToReleased(memberWallet.getId(), memberWallet.getToReleased());
            if(i==0){
                return MessageResult.error("解除合约失败");
            }
            insertDetile( memberWallet.getToReleased().negate(),memberWallet.getMemberId(),memberWallet.getCoin().getName(),TransactionType.RESCISSION_OF_CONTRACT);
            //查看用户30天以内
            BigDecimal paymentFee = memberTransactionService.getPaymentFee(id,coin);
            BigDecimal mul = FunctionUtils.mul(paymentFee, memberWallet.getToReleased(), 4);
            BigDecimal sub = FunctionUtils.sub(memberWallet.getToReleased(), mul, 2);
            //增加用户钱包可用余额
            memberWalletDao.increaseBalance(memberWallet.getId(),sub);
            insertDetile( memberWallet.getToReleased(),memberWallet.getMemberId(),memberWallet.getCoin().getName(),TransactionType.RESCISSION_OF_CONTRACT);
            return MessageResult.success("解除成功");
        }else {
            return MessageResult.error("操作有误");
        }
    }

    /**
     * 功能描述:保存明细
     * @param:
     * @return:
     */
    public void insertDetile(BigDecimal price,Long memberId,String currency,TransactionType type){
        //插入明细
        MemberTransaction memberTransaction=new MemberTransaction();
        memberTransaction.setAmount(price);
        memberTransaction.setFee(BigDecimal.ZERO);
        memberTransaction.setMemberId(memberId);
        memberTransaction.setSymbol(currency);
        memberTransaction.setType(type);
        memberTransactionDao.save(memberTransaction);
    }
}

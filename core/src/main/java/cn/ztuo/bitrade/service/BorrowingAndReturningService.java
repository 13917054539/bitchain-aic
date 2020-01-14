package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.ActivityRewardType;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.controller.BaseController;
import cn.ztuo.bitrade.dao.*;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.util.Md5;
import cn.ztuo.bitrade.util.MessageResult;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther:路道
 * @Date:2019/6/18
 * @Description:cn.ztuo.bitrade.service
 * @version:1.0
 */
@Service
public class BorrowingAndReturningService extends BaseController {

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private MemberBorrowingReturningDao memberBorrowingReturningDao;

    @Autowired
    private MemberWalletService memberWalletService;

    @Autowired
    private MemberWalletDao memberWalletDao;

    @Autowired
    private MemberTransactionDao memberTransactionDao;

    @Autowired
    private RewardActivitySettingService rewardActivitySettingService;

    @Autowired
    private MemberService memberService;
    @Autowired
    private LocaleMessageSourceService sourceService;
    /**
     * 功能描述:
     * @param: id 用户ID
     * @param: type 交易类型 0借币1还币
     * @param: currencyNum 交易数量
     * @return:
     */
    @Transactional
    public synchronized MessageResult borrowingAndReturning(long id, Integer type, String currencyNum,String pwd) {
        if(StringUtils.isEmpty(type)){
            return MessageResult.error("请选择操作类型");
        }
        if(StringUtils.isEmpty(pwd)){
            return MessageResult.error("请输入密码");
        }
        Member member = memberService.findOne(id);
        String mbPassword = member.getJyPassword();
        try {
            Assert.isTrue(Md5.md5Digest(pwd + member.getSalt()).toLowerCase().equals(mbPassword), sourceService.getMessage("ERROR_JYPASSWORD"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(type==0){
            if(StringUtils.isEmpty(currencyNum)){
                return MessageResult.error("请输入交易数量");
            }
            //查看用户的直推人数是否有5个有效会员且有效会员不能有借款记录
            int num = memberDao.effectiveMembership(id);
            if(num<5){
                return MessageResult.error("请先直推5个有效会员");
            }
            //查看用户是否有未还的交易借币记录
            int detileNum = memberBorrowingReturningDao.borrowingRecordDetile(id);
            if(detileNum>0){
                return MessageResult.error("您有一笔借款记录暂未归还");
            }
            //获取借币买币的交易配置信息
            RewardActivitySetting byType = rewardActivitySettingService.findByType(ActivityRewardType.BORROWINGANDRETURNING);
            JSONObject jsonObject = JSONObject.fromObject(byType.getInfo());
            //获取最大值
            Integer max = jsonObject.getInt("MAX");
            //获取最小值
            Integer min = jsonObject.getInt("MIN");
            //判断用户借款数量是否满足配置
            if(new BigDecimal(currencyNum).compareTo(new BigDecimal(max))==1 ){
                return MessageResult.error("您当前借款金额不能超过："+max);
            }
            if(new BigDecimal(currencyNum).compareTo(new BigDecimal(min))==-1 ){
                return MessageResult.error("借款金额不得低于："+min);
            }
            //增加账户余额
            //获取用户钱包信息（WBC）
            MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId("WBC", id);
            //增加余额
            memberWalletDao.increaseBalance(memberWallet.getId(),new BigDecimal(currencyNum));
            //插入余额明细
            MemberTransaction memberTransaction=new MemberTransaction();
            memberTransaction.setAmount(new BigDecimal(currencyNum));
            memberTransaction.setFee(BigDecimal.ZERO);
            memberTransaction.setMemberId(id);
            memberTransaction.setSymbol("WBC");
            memberTransaction.setType(TransactionType.BORROW_MONEY);
            memberTransactionDao.save(memberTransaction);
            //插入记录
            MemberBorrowingReturning memberBorrowingReturning=new MemberBorrowingReturning();
            memberBorrowingReturning.setMid(id);
            memberBorrowingReturning.setCreatetime(new Date());
            memberBorrowingReturning.setWbcnum(new BigDecimal(currencyNum));
            memberBorrowingReturning.setType(type);
            memberBorrowingReturning.setStatus(0);
            memberBorrowingReturningDao.save(memberBorrowingReturning);
        }else if(type==1){
            //查看用户的欠款信息
            MemberBorrowingReturning memberBorrowingReturning = memberBorrowingReturningDao.InformationOnArrears(id);
            if(StringUtils.isEmpty(memberBorrowingReturning)){
                return  MessageResult.error("暂无欠款信息");
            }
            //获取用户钱包信息（WBC）
            MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId("WBC", id);
            if(memberWallet.getBalance().compareTo(memberBorrowingReturning.getWbcnum())>=0){
                int i = memberWalletDao.decreaseBalance(memberWallet.getId(), memberBorrowingReturning.getWbcnum());
                if(i==0){
                    return  MessageResult.error("还款失败");
                }
                //插入余额明细
                MemberTransaction memberTransaction=new MemberTransaction();
                memberTransaction.setAmount(memberBorrowingReturning.getWbcnum().negate());
                memberTransaction.setFee(BigDecimal.ZERO);
                memberTransaction.setMemberId(id);
                memberTransaction.setSymbol("WBC");
                memberTransaction.setType(TransactionType.RETURN_CURRENCY);
                memberTransactionDao.save(memberTransaction);
                //修改借款状态
                memberBorrowingReturning.setStatus(1);
                memberBorrowingReturning.setReturntime(new Date());
                memberBorrowingReturningDao.save(memberBorrowingReturning);

            }else {
                return MessageResult.error("余额不足");
            }

        }else {
            return MessageResult.error("操作类型有误请重新选择");
        }

        return MessageResult.success();
    }

    /**
     * 功能描述: 查看用户最多可借金额
     * @param:
     * @return:
     */
    public MessageResult borrowingAndReturningConf(long id) {
        //获取借币买币的交易配置信息
        RewardActivitySetting byType = rewardActivitySettingService.findByType(ActivityRewardType.BORROWINGANDRETURNING);
        JSONObject jsonObject = JSONObject.fromObject(byType.getInfo());
        //获取最大值
        Integer max = jsonObject.getInt("MAX");
        int detileNum = memberBorrowingReturningDao.borrowingRecordDetile(id);
        Map<String,Object> map=new HashMap();
        if(detileNum>0){
            //查看用户的欠款信息
            MemberBorrowingReturning memberBorrowingReturning = memberBorrowingReturningDao.InformationOnArrears(id);
            map.put("oweMoney",memberBorrowingReturning.getWbcnum());
            map.put("max",0);
        }else {
            map.put("max",max);
            map.put("oweMoney",0);
        }
        return success(map);
    }
}

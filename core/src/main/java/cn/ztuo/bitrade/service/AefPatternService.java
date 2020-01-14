package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.controller.BaseController;
import cn.ztuo.bitrade.dao.AefPatternConfDao;
import cn.ztuo.bitrade.dao.MemberDao;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.MemberWalletDao;
import cn.ztuo.bitrade.entity.AefPatternConf;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.MemberWallet;
import cn.ztuo.bitrade.util.FunctionUtils;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther:路道
 * @Date:2019/8/6
 * @Description:cn.ztuo.bitrade.service
 * @version:1.0
 */
@Slf4j
@Service
public class AefPatternService extends BaseController {

    @Autowired
    private MemberWalletDao memberWalletDao;
    @Autowired
    private AefPatternConfDao aefPatternConfDao;
    @Autowired
    private BIPBMathService bIPBMathService;
    @Autowired
    private MemberTransactionDao memberTransactionDao;
    @Autowired
    private MemberDao memberDao;

    /**
     * 功能描述: 注册奖励
     * @param:
     * @return:
     */
    @Transactional
    public void  registrationAward(long id,long parentid){
        log.info(">>>>>>>>>>进入注册奖励>>>>>>>>>>");
        //获取用户CMB钱包信息
        MemberWallet memberWallet= memberWalletDao.memberWalletCmb(id);
        //获取用户CTO余额信息
        MemberWallet memberWalletCto= memberWalletDao.memberWalletCto(id);

        if(!StringUtils.isEmpty(memberWallet)){
            log.info(">>>>>>>>>>注册奖励--->>>>>>>>>>>");
            //查看注册奖励配置
            AefPatternConf aefPatternConf = aefPatternConfDao.getAefPatternConf(1);
            if(aefPatternConf.getRegistrationaward().compareTo(BigDecimal.ZERO)==1){
                //赠送CMB到静态释放账户上
            bIPBMathService.updateToReleased(memberWallet.getId(),aefPatternConf.getRegistrationaward(),1,memberWallet.getCoin().getName(), TransactionType.REGISTRATION_AWARD);
            bIPBMathService.updateToReleased(memberWalletCto.getId(),aefPatternConf.getCtoregistrationaward(),1,memberWalletCto.getCoin().getName(), TransactionType.REGISTRATION_AWARD);
            //获取推荐人信息
            Member memberById = memberDao.findMemberById(parentid);
            if(memberById.getRealNameStatus().getOrdinal()==2){
                //获取推荐人余额信息
                MemberWallet parentMemberWalletCto= memberWalletDao.memberWalletCto(parentid);
                bIPBMathService.updateToReleased(parentMemberWalletCto.getId(),aefPatternConf.getCtorecommendationaward(),1,parentMemberWalletCto.getCoin().getName(), TransactionType.CTORECOMMENDATIONAWARD);
            }

        }
            log.info(">>>>>>>>>奖励结束>>>>>>>>>>");
        }

    }

    /**
     * 功能描述:签到
     * @param:
     * @return:
     */
    @Transactional
    public synchronized MessageResult signIn(long id) {
        //判断用户是否有直推10个有效会员
        Integer integer = memberDao.zhiTuiRenShu(id);
        //查看签到奖励配置
        AefPatternConf aefPatternConf = aefPatternConfDao.getAefPatternConf(1);
        if(integer<aefPatternConf.getNumbersatisfaction()){
            return MessageResult.error("请在一个月之内直推"+aefPatternConf.getNumbersatisfaction()+"个有效会员");
        }
        //查看用户今日是否已经签到
        BigDecimal bigDecimal = memberTransactionDao.toDayamountNum(id, TransactionType.SIGN_IN_AWARD.getOrdinal());
        if(bigDecimal.compareTo(BigDecimal.ZERO)==1){
            return MessageResult.error("您今日已签到");
        }
        //获取用户CMB钱包信息
        MemberWallet memberWalletCmb = memberWalletDao.memberWalletCmb(id);
        //可释放金额
        BigDecimal shiFangPrice=BigDecimal.ZERO;
        if(memberWalletCmb.getToReleased().compareTo(new BigDecimal("28"))!=1){
            shiFangPrice=memberWalletCmb.getToReleased();
        }else {
            if(memberWalletCmb.getMemberId()<12269){
                shiFangPrice=new BigDecimal("10000").divide(aefPatternConf.getSigninaward(),2,BigDecimal.ROUND_DOWN);
            }else {
                // shiFangPrice= FunctionUtils.div(aefPatternConf.getRegistrationaward(),aefPatternConf.getSigninaward(),0);
                shiFangPrice=aefPatternConf.getRegistrationaward().divide(aefPatternConf.getSigninaward(),2,BigDecimal.ROUND_DOWN);
            }
        }
        if(shiFangPrice.compareTo(BigDecimal.ZERO)==1){
            //扣除释放余额
            bIPBMathService.updateToReleased(memberWalletCmb.getId(),shiFangPrice,0,memberWalletCmb.getCoin().getName(),TransactionType.SIGN_IN_AWARD);
            //增加可转数量
            bIPBMathService.updateConvertible(memberWalletCmb.getId(),shiFangPrice,1,memberWalletCmb.getCoin().getName(),TransactionType.SIGN_IN_AWARD);
        }
        return  MessageResult.success("签到成功");
    }


    /**
     * 功能描述:查看用户今日是否已经签到  0是已签到  1是未签到
     * @param:
     * @return:
     */
    public MessageResult isSignIn(long id) {
        BigDecimal bigDecimal = memberTransactionDao.toDayamountNum(id, TransactionType.SIGN_IN_AWARD.getOrdinal());
        if(bigDecimal.compareTo(BigDecimal.ZERO)==1){
            return success(0);
        }
        return success(1);
    }


      /**
     * 功能描述: 显示用户资产信息
     * @param:
     * @return:
     */
    public MessageResult showAcount(long id) {
        //获取用户CMB钱包信息
        MemberWallet memberWalletCmb = memberWalletDao.memberWalletCmb(id);
        Map<String,Object> map = new HashMap<String,Object>();
        return success(memberWalletCmb);
    }

    /**
     * 功能描述: 转换
     * @param:
     * @return:
     */
    @Transactional
    public synchronized MessageResult transformation(long id) {
        //获取用户CMB钱包信息
        MemberWallet memberWalletCmb = memberWalletDao.memberWalletCmb(id);
        if(memberWalletCmb.getConvertible().compareTo(BigDecimal.ZERO)!=1){
            return MessageResult.error("可转数量不足");
        }
        if(memberWalletCmb.getConvertiblequota().compareTo(BigDecimal.ZERO)!=1){
            return MessageResult.error("当前无可转额度");
        }
        //操作数量
        BigDecimal cost=BigDecimal.ZERO;
        if(memberWalletCmb.getConvertiblequota().compareTo(memberWalletCmb.getConvertible())>=0){
            cost=memberWalletCmb.getConvertible();
        }else {
            cost=memberWalletCmb.getConvertiblequota();
        }

        //扣除可转数量
        bIPBMathService.updateConvertible(memberWalletCmb.getId(),cost,0,memberWalletCmb.getCoin().getName(),TransactionType.CMB_ROLL_OUT);
        //扣除额度
        bIPBMathService.updateConvertiblequota(memberWalletCmb.getId(),cost,0,memberWalletCmb.getCoin().getName(),TransactionType.QUOTA_TRANSFER);
        //增加CMB余额数量
        bIPBMathService.updateBalance(memberWalletCmb.getId(),cost,1,memberWalletCmb.getCoin().getName(),TransactionType.CMB_ROLL_OUT);
        return success("操作成功");
    }

}

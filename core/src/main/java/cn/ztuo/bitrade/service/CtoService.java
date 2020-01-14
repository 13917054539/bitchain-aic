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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther:路道
 * @Date:2019/8/30
 * @Description:cn.ztuo.bitrade.service
 * @version:1.0
 */
@Service
public class CtoService extends BaseController {

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
     * 功能描述: 进入CTO界面
     * @param:
     * @return:
     */
    public MessageResult showCto(long id) {
        //查看当前币价信息
        AefPatternConf aefPatternConf = aefPatternConfDao.getAefPatternConf(1);
        //查看用户CMB钱包信息
        MemberWallet memberWalletCmb = memberWalletDao.memberWalletCmb(id);
        //查看用户CTO钱包信息
        MemberWallet memberWalletCto =memberWalletDao.memberWalletCto(id);
        Map<String,Object> map=new HashMap<>();
        //CTO币价
        map.put("cto",aefPatternConf.getCtocurrency());
        //待领取CTO
        map.put("toReleased",memberWalletCto.getToReleased());
        //CTO额度
        map.put("ctoQuota",memberWalletCto.getCtoquota());
        //可转换的CTO数量
        map.put("convertible",memberWalletCmb.getConvertible());
        return success(map);
    }

    /**
     * 功能描述:签到CTO
     * @param:
     * @return:
     */
    public MessageResult ctoSignIn(long id) {
        //查看用户今日是否已经签到
        BigDecimal bigDecimal = memberTransactionDao.toDayamountNum(id, TransactionType.CTO_SIGN_IN_AWARD.getOrdinal());
        if(bigDecimal.compareTo(BigDecimal.ZERO)==1){
            return MessageResult.error("您今日已签到");
        }
        //查看签到奖励配置
        AefPatternConf aefPatternConf = aefPatternConfDao.getAefPatternConf(1);
        //查看用户的推荐人数
        Integer countDirectpush = memberDao.getCountDirectpush(id);
        //计算累计注册的奖励
        BigDecimal mul = FunctionUtils.mul(new BigDecimal(countDirectpush), aefPatternConf.getCtoregistrationaward(), 4);

        //获取用户CTO钱包信息
        MemberWallet memberWalletCto = memberWalletDao.memberWalletCto(id);
        //可释放金额
        BigDecimal shiFangPrice=BigDecimal.ZERO;
        if(memberWalletCto.getToReleased().compareTo(new BigDecimal("1"))!=1){
            shiFangPrice=memberWalletCto.getToReleased();
        }else {
            BigDecimal add = FunctionUtils.add(aefPatternConf.getCtoregistrationaward(), mul, 2);
            shiFangPrice=FunctionUtils.mul(add,aefPatternConf.getCtosigninaward(),2);
        }
        if(shiFangPrice.compareTo(BigDecimal.ZERO)==1){
            if(shiFangPrice.compareTo(memberWalletCto.getToReleased())==1){
                shiFangPrice=memberWalletCto.getToReleased();
            }
            //扣除释放余额
            bIPBMathService.updateToReleased(memberWalletCto.getId(),shiFangPrice,0,memberWalletCto.getCoin().getName(),TransactionType.CTO_SIGN_IN_AWARD);
            //增加余额
            bIPBMathService.updateBalance(memberWalletCto.getId(),shiFangPrice,1,memberWalletCto.getCoin().getName(),TransactionType.CTO_SIGN_IN_AWARD);
        }
        return  MessageResult.success("签到成功");
    }

    /**
     * 功能描述: 查看用户今日是否已经签到 0是已签到  1是未签到
     * @param:
     * @return:
     */
    public MessageResult isCtoSignIn(long id) {
        BigDecimal bigDecimal = memberTransactionDao.toDayamountNum(id, TransactionType.CTO_SIGN_IN_AWARD.getOrdinal());
        if(bigDecimal.compareTo(BigDecimal.ZERO)==1){
            return success(0);
        }
        return success(1);
    }

    /**
     * 功能描述: CMB转CTO
     * @param:
     * @return:
     */
    public MessageResult transformation(long id,String num) {
        if(StringUtils.isEmpty(num)){
            return MessageResult.error("请输入转换数量");
        }
        if(new BigDecimal(num).compareTo(BigDecimal.ZERO)!=1){
            return MessageResult.error("输入转换数量有误");
        }
        //查看用户CMB钱包信息
        MemberWallet memberWalletCmb = memberWalletDao.memberWalletCmb(id);
        if(memberWalletCmb.getConvertible().compareTo(new BigDecimal(num))<=0){
            return MessageResult.error("可转数量不足");
        }
        //查看转换配置信息
        AefPatternConf aefPatternConf = aefPatternConfDao.getAefPatternConf(1);
      //  BigDecimal mul = FunctionUtils.mul(new BigDecimal(num), aefPatternConf.getCtosigninaward(), 4);
        BigDecimal divide = new BigDecimal(num).divide(aefPatternConf.getCtocurrency(), 4, BigDecimal.ROUND_DOWN);
        //获取用户CTO钱包信息
        MemberWallet memberWalletCto = memberWalletDao.memberWalletCto(id);

        //扣除CMB可转数量
        bIPBMathService.updateConvertible(memberWalletCmb.getId(),new BigDecimal(num),0,memberWalletCmb.getCoin().getName(),TransactionType.TRANSFORMATION);

        //增加CTO余额
        bIPBMathService.updateBalance(memberWalletCto.getId(),divide,1,memberWalletCto.getCoin().getName(),TransactionType.TRANSFORMATION);

        return MessageResult.success("转换成功");
    }
    
    /** 
     * 功能描述:计算CTO内盘价
     * @param: 
     * @return:
     */
    public void internalPrice() {
        //查看CTO内盘价配置信息
        AefPatternConf aefPatternConf = aefPatternConfDao.getAefPatternConf(1);
        BigDecimal mul = FunctionUtils.mul(aefPatternConf.getCtocurrency(), aefPatternConf.getCtoincrease(), 8);
        aefPatternConf.setCtocurrency(FunctionUtils.add(aefPatternConf.getCtocurrency(), mul, 8));
        aefPatternConfDao.save(aefPatternConf);
    }
}



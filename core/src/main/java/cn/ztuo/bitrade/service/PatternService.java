package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.ActivityRewardType;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.MemberDao;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.MemberWalletDao;
import cn.ztuo.bitrade.dao.RewardActivitySettingDao;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.MemberTransaction;
import cn.ztuo.bitrade.entity.MemberWallet;
import cn.ztuo.bitrade.entity.RewardActivitySetting;
import cn.ztuo.bitrade.util.FunctionUtils;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * @Auther:路道
 * @Date:2019/6/19
 * @Description:cn.ztuo.bitrade.service
 * @version:1.0
 */
@Service
public class PatternService {

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private RewardActivitySettingService rewardActivitySettingService;

    @Autowired
    private MemberWalletDao memberWalletDao;

    @Autowired
    private MemberTransactionDao memberTransactionDao;

    @Autowired
    private MemberWalletService memberWalletService;


    /**
     * 功能描述: 社区奖励
     * @param: id 用户ID
     * @return: fee 交易手续费
     */
    @Transactional(rollbackFor = Exception.class)
    public void directPushAward(long id, BigDecimal fee){
        //查看会员信息
        Member memberById = memberDao.findMemberById(id);
        //查看最靠近用户的3个VIP1到vip3的会员信息
        List<Member> members = memberDao.vipMember( Arrays.asList(memberById.getGenes().split(",")));
        if(StringUtils.isEmpty(members)|| members.size()==0){
            return;
        }
        //VIP1奖金比例
        BigDecimal vipOne=BigDecimal.ZERO;
        //VIP2奖金比例
        BigDecimal vipTwo=BigDecimal.ZERO;
        //VIP3奖金比例
        BigDecimal vipThree=BigDecimal.ZERO;
        //查看VIP1升级配置信息
        RewardActivitySetting vipOneConf = rewardActivitySettingService.findByType(ActivityRewardType.VIPONECONF);
        JSONObject jsonVipOneConf = JSONObject.fromObject(vipOneConf.getInfo());
        BigDecimal oneRatio=new BigDecimal(jsonVipOneConf.getString("oneRatio"));
        //查看VIP2升级配置信息
        RewardActivitySetting vipTwoConf = rewardActivitySettingService.findByType(ActivityRewardType.VIPTWOCONF);
        JSONObject jsonVipTwoConf = JSONObject.fromObject(vipTwoConf.getInfo());
        BigDecimal twoRatio=new BigDecimal(jsonVipTwoConf.getString("twoRatio"));
        //查看VIP3升级配置信息
        RewardActivitySetting vipThreeConf = rewardActivitySettingService.findByType(ActivityRewardType.VIPTHREECONF);
        JSONObject jsonVipThreeConf = JSONObject.fromObject(vipThreeConf.getInfo());
        BigDecimal threeRatio=new BigDecimal(jsonVipThreeConf.getString("threeRatio"));
        for (Member m:members){
          if(m.getVip()==3){
              //算出VIP实际的奖金比例
              BigDecimal sub = FunctionUtils.sub(threeRatio, vipTwo, 2);
              vipThree= FunctionUtils.sub(sub,vipOne,2);
              if(vipThree.compareTo(BigDecimal.ZERO)==1){
                  //算出奖励金额
                  BigDecimal mul = FunctionUtils.mul(vipThree, fee, 2);
                  if(mul.compareTo(BigDecimal.ZERO)==1){
                      MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId("WBC", m.getId());
                      memberWalletDao.increaseBalance(memberWallet.getId(),mul);
                      insertTransaction(m.getId(),mul);
                  }
              }
          }else if(m.getVip()==2){
              vipTwo= FunctionUtils.sub(twoRatio,oneRatio,2);
              if(vipTwo.compareTo(BigDecimal.ZERO)==1){
                  //算出奖励金额
                  BigDecimal mul = FunctionUtils.mul(vipTwo, fee, 2);
                  if(mul.compareTo(BigDecimal.ZERO)==1){
                      MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId("WBC", m.getId());
                      memberWalletDao.increaseBalance(memberWallet.getId(),mul);
                      insertTransaction(m.getId(),mul);
                  }
              }
          }else if(m.getVip()==1){
              vipOne=oneRatio;
              if(vipOne.compareTo(BigDecimal.ZERO)==1){
                  //算出奖励金额
                  BigDecimal mul = FunctionUtils.mul(vipTwo, fee, 2);
                  if(mul.compareTo(BigDecimal.ZERO)==1){
                      MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId("WBC", m.getId());
                      memberWalletDao.increaseBalance(memberWallet.getId(),mul);
                      insertTransaction(m.getId(),mul);
                  }
              }
          }
        }


    }


    /**
     * 功能描述: 升级
     * @param: id 用户ID
     * @return:
     */
    public void upgrade(Integer id){
        //查看会员信息
        Member memberById = memberDao.findMemberById(id);
        //查看用户线上的所有人员信息及他们的直推人数和团队人数(有效)
        List<Member> members =  memberDao.upperMember( Arrays.asList(memberById.getGenes().split(",")));
        //查看VIP1升级配置信息
        RewardActivitySetting vipOneConf = rewardActivitySettingService.findByType(ActivityRewardType.VIPONECONF);
        JSONObject jsonVipOneConf = JSONObject.fromObject(vipOneConf.getInfo());
        //查看VIP2升级配置信息
        RewardActivitySetting vipTwoConf = rewardActivitySettingService.findByType(ActivityRewardType.VIPTWOCONF);
        JSONObject jsonVipTwoConf = JSONObject.fromObject(vipTwoConf.getInfo());
        //查看VIP3升级配置信息
        RewardActivitySetting vipThreeConf = rewardActivitySettingService.findByType(ActivityRewardType.VIPTHREECONF);
        JSONObject jsonVipThreeConf = JSONObject.fromObject(vipThreeConf.getInfo());
        //满足条件的升级
        for (Member m:members) {
            //查看用户团队人数
            Integer teamnum = memberDao.teamnum(m.getId());
            //查看用户直推人数
            Integer zhituinum = memberDao.zhituinum(m.getId());
            Integer vip=0;
            if(zhituinum>=jsonVipThreeConf.getInt("vipThreeZhiTuiNum")&& teamnum>=jsonVipThreeConf.getInt("vipThreeTeamNum")){
                vip=3;
            }else if(zhituinum>=jsonVipThreeConf.getInt("vipTwoZhiTuiNum")&& teamnum>=jsonVipThreeConf.getInt("vipTwoTeamNum")){
                vip=2;
            }else if(zhituinum>=jsonVipThreeConf.getInt("vipOneZhiTuiNum")&& teamnum>=jsonVipThreeConf.getInt("vipOneZhiTuiNum")){
                vip=1;
            }
            if(vip>0){
                m.setVip(vip);
                memberDao.save(m);
            }
        }
    }


    /**
     * 功能描述: 插入余额明细
     * @param:
     * @return:
     */
    public void insertTransaction(long id,BigDecimal money){
        MemberTransaction memberTransaction=new MemberTransaction();
        memberTransaction.setAmount(money);
        memberTransaction.setFee(BigDecimal.ZERO);
        memberTransaction.setMemberId(id);
        memberTransaction.setSymbol("WBC");
        memberTransaction.setType(TransactionType.COMMUNITY_REWARDS);
        memberTransactionDao.save(memberTransaction);
    }
}

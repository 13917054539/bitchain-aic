package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.component.CoinExchangeRate;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.*;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.util.FunctionUtils;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @Auther:路道
 * @Date:2019/6/26
 * @Description:cn.ztuo.bitrade.service
 * @version:1.0
 */
@Service
public class ReleaseService {

    @Value("${platform.token}")
    private String platformToken;

    @Autowired
    private  MemberWalletService memberWalletService;

    @Autowired
    private MemberWalletDao memberWalletDao;

    @Autowired
    private MemberTransactionDao memberTransactionDao;

    @Autowired
    private PatternConfDao patternConfDao;

    @Autowired
    private CoinExchangeRate coinExchangeRate;

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private AefPatternConfDao aefPatternConfDao;

    @Autowired
    private BIPBMathService bIPBMathService;

    /**
     * 功能描述:释放
     * @param:
     * @return:
     */
    @Transactional
    public void release(){
        //获取系统中所有的钱包信息
        List<MemberWallet> memberWallets = memberWalletDao.allMemberWallet();
        try {
            //同步汇率信息
            coinExchangeRate.realSyncPrice();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        //获取释放比例配置
        AefPatternConf aefPatternConf = aefPatternConfDao.getAefPatternConf(1);
        for (MemberWallet memberWallet:memberWallets){
            //根据币种信息获取对应的转换USDT的汇率
            BigDecimal usdRate = coinExchangeRate.getUsdRate(memberWallet.getCoin().getName());
            //算出可转换USDT的数量
            BigDecimal usdtNum = FunctionUtils.mul(memberWallet.getToReleased(), usdRate, 4);
            if(usdtNum.compareTo(aefPatternConf.getMaxnum())>=0){
                //获取USDT转平台币的汇率
                BigDecimal usdt = coinExchangeRate.getUsdRate(platformToken);
                BigDecimal div=usdtNum.divide(usdt,4,BigDecimal.ROUND_DOWN);
                //计算出释放的数量
                BigDecimal mul1 = FunctionUtils.mul(aefPatternConf.getReleaseratio(), div, 4);
                //增加平台币余额
                MemberWallet wallet = memberWalletDao.memberWalletCmb(memberWallet.getMemberId());
                bIPBMathService.updateBalance(wallet.getId(),mul1,1,wallet.getCoin().getName(),TransactionType.INTEREST_RATE_INCREASE_AWARD);
                //增加可转额度
                bIPBMathService.updateConvertiblequota(wallet.getId(),mul1,1,wallet.getCoin().getName(),TransactionType.INCREASED_QUOTA);
                //层级奖
                Member memberById = memberDao.findMemberById(memberWallet.getMemberId());
                hierarchicalAward(memberById,aefPatternConf,mul1);
            }
    }

    }

    /**
     * 功能描述: 阿尔法层级奖
     * @param:
     * @return:
     */
    public void hierarchicalAward(Member member,AefPatternConf aefPatternConf,BigDecimal money){
        //查看用户上面6层的用户信息
        String[] split = member.getGenes().split(",");
        //获取用户基因链上所有的上级用户信息（有效会员）
        List<Member> members = memberDao.parentssMember(Arrays.asList(split));
        //层数(6层结束)
        int layerNumber=0;
        for (Member patternMember:members){
            layerNumber=layerNumber+1;
            if(layerNumber>6){
                return;
            }
           BigDecimal proportion=BigDecimal.ZERO;
           if(layerNumber==1){
               proportion=aefPatternConf.getOnereward();
           }else if(layerNumber==2){
               proportion=aefPatternConf.getTworeward();
           }else if(layerNumber==3){
               proportion=aefPatternConf.getThreereward();
           }else if(layerNumber==4){
               proportion=aefPatternConf.getFourreward();
           }else if(layerNumber==5){
               proportion=aefPatternConf.getFivereward();
           }else if(layerNumber==6){
               proportion=aefPatternConf.getSixreward();
           }
           if(proportion.compareTo(BigDecimal.ZERO)==1 && flag(patternMember.getId(),aefPatternConf)){
               //增加CMB余额
               BigDecimal mul = FunctionUtils.mul(money, proportion, 4);
               MemberWallet memberWallet = memberWalletDao.memberWalletCmb(patternMember.getId());
               bIPBMathService.updateBalance(memberWallet.getId(),mul,1,memberWallet.getCoin().getName(),TransactionType.LEVEL_AWARD);
           }


        }
    }

    /**
     * 功能描述: 查看用户是否是有效会员
     * @param:
     * @return:
     */
    public Boolean flag(Long id,AefPatternConf aefPatternConf){
        //获取用户所有的钱包信息
        List<MemberWallet> allByMemberId = memberWalletDao.findAllByMemberId(id);
        BigDecimal usdt=BigDecimal.ZERO;
        for (MemberWallet memberWallet:allByMemberId){
            //根据币种信息获取对应的转换USDT的汇率
            BigDecimal cnyRate = coinExchangeRate.getUsdRate(memberWallet.getCoin().getName());
            if(memberWallet.getCoin().getName().equals(platformToken)){
                //算出可转换USDT的数量
                BigDecimal usdtNum = FunctionUtils.mul(memberWallet.getBalance(), cnyRate, 4);
                usdt=FunctionUtils.add(usdtNum,usdt,8);
            }else {
                //算出可转换USDT的数量
                BigDecimal usdtNum = FunctionUtils.mul(FunctionUtils.add(memberWallet.getBalance(),memberWallet.getToReleased(),4), cnyRate, 4);
                usdt=FunctionUtils.add(usdtNum,usdt,8);
            }
        }
        if(usdt.compareTo(aefPatternConf.getSatisfyingassets())>=0){
            return true;
        }
        return false;
    }

    /** 
     * 功能描述: 层级奖励
     * @param: id 用户ID
     * @param: price 奖金基数（根据释放数量得出）
     * @return:
     */
    public void hierarchicalRewards(long id,BigDecimal price, PatternConf patternConf){
        //根据用户ID获取推荐人信息（有效会员）
        Member parentUser = memberDao.parentUser(id);
        if(StringUtils.isEmpty(parentUser)){
            return;
        }
        //判断推荐人是否是有效会员
        if(EffectiveMembership(parentUser.getId(),patternConf)){
            //满足就给直推奖励
            BigDecimal zhiTuiPrice = FunctionUtils.mul(price, patternConf.getDirectrevenue(), 4);
            if(zhiTuiPrice.compareTo(BigDecimal.ZERO)==1){
                updateBalance(parentUser.getId(),platformToken,zhiTuiPrice,1,TransactionType.DIRECT_PUSH_AWARD);
            }
        }

        //获取用户基因链上所有的上级用户信息（有效会员）
        //List<Member> members = memberDao.genesMember(parentUser.getId());
        //获取用户基因链上所有的上级用户信息（有效会员）
        String[] split = parentUser.getGenes().split(",");
        List<Member> members = memberDao.parentssMember(Arrays.asList(split));
        //层数
        int layerNumber=1;
        //会员级别
        int level=0;
        //有效会员数量
        int effectiveMembership=0;
        //奖励比例
        BigDecimal proportion=BigDecimal.ZERO;
        for (Member member:members){
            effectiveMembership=0;
            layerNumber=layerNumber+1;
            proportion=BigDecimal.ZERO;
            //满足层数之后就开始返无限奖励
            if(layerNumber>patternConf.getLayernumber()){
               if(EffectiveMembership(member.getId(),patternConf)) {
                   int memberLevel = getMemberLevel(member, patternConf);
                   if(memberLevel>level){
                       level=memberLevel;
                       if (memberLevel == 3) {
                           proportion = patternConf.getThreeproportion();
                       } else if (memberLevel == 2) {
                           proportion = patternConf.getTwoproportion();
                       } else if (memberLevel == 1) {
                           proportion = patternConf.getOneproportion();
                       }
                       if (proportion.compareTo(BigDecimal.ZERO) == 1) {
                           //开始返利
                           BigDecimal mul = FunctionUtils.mul(price, proportion, 5);
                           if(mul.compareTo(BigDecimal.ZERO)==1){
                               updateBalance(member.getId(), platformToken, mul, 1, TransactionType.INFINITE_REWARD);
                           }
                       }
                   }
               }
                continue;
            }
            //判断用户自身是否是有效会员
            if(EffectiveMembership(member.getId(),patternConf)){
                //查看用户的直推会员的信息
                List<Member> zhiTuiMembers = memberDao.zhiTuiMember(member.getId());
                //查看用户直推的会员有几个有效会员
                for (Member zhiTuiMember:zhiTuiMembers){
                    if(EffectiveMembership(zhiTuiMember.getId(),patternConf)){
                        effectiveMembership = effectiveMembership+1;
                    }
                }
                //判断用户直推的有效会员是否大于等于当前的层级数
                if(effectiveMembership>=layerNumber){
                    int memberLevel = getMemberLevel(member, patternConf);
                    if(level==0 || memberLevel>level){
                        proportion=patternConf.getHierarchicalincome();
                     }
                     if(memberLevel>level){
                         level=memberLevel;
                         if (memberLevel == 3) {
                             proportion = patternConf.getThreeproportion();
                         } else if (memberLevel == 2) {
                             proportion = patternConf.getTwoproportion();
                         } else if (memberLevel == 1) {
                             proportion = patternConf.getOneproportion();
                         }
                     }
                    /* if(memberLevel>0){
                         level=memberLevel;
                     }*/
                }
            }
            if(proportion.compareTo(BigDecimal.ZERO) == 1){
                //开始返利
                BigDecimal mul = FunctionUtils.mul(price, proportion, 5);
                if(mul.compareTo(BigDecimal.ZERO)==1){
                    updateBalance(member.getId(), platformToken, mul, 1, TransactionType.HIERARCHICAL_AWARD);
                }
            }
        }

    }

    /**
     * 功能描述:加权奖励
     * @param:
     * @return:
     */
    public void weightedReward(){
        //获取今日平台释放的总量
        BigDecimal bigDecimal = memberTransactionDao.totalRelease();
        if(bigDecimal.compareTo(BigDecimal.ZERO)!=1){
            return;
        }
        //获取加权分红的奖励配置
        PatternConf patternConf = getPatternConf();
        //计算出可拿出来分红的数量
        BigDecimal mul = FunctionUtils.mul(bigDecimal, patternConf.getWeightedreward(), 5);
        if(mul.compareTo(BigDecimal.ZERO)!=1){
            return;
        }
        List<Member> sp=new ArrayList<Member>();

        //查看系统中所有的会员信息
        List<Member> members = memberDao.memberAll();
        for (Member member:members){
            //查看用户自身是否是有效会员
            if(EffectiveMembership(member.getId(),patternConf)){
                //查看用户下面有几个S3
                int eachLayerThree = getEachLayerThree(member, patternConf);
                if(eachLayerThree>=patternConf.getHavingthree()){
                    sp.add(member);
                }
            }
        }
        if(sp.size()==0){
            return;
        }
        //计算每个人可平均分到的金钱
        BigDecimal div = FunctionUtils.div(mul, new BigDecimal(sp.size()), 5);
        for (Member member:sp){
            updateBalance(member.getId(), platformToken, div, 1, TransactionType.KEEPING_MONEY_TO_EARN_INTEREST);
        }
    }

    /**
     * 功能描述:
     * @param:
     * @return:
     */
    public Boolean teamAchievement(Member member,PatternConf patternConf){
        //查看用户团队10层内的用户ID
        List<Long> longId = memberDao.inLayer(member.getId(), member.getGeneration(), patternConf.getTeamlayer());
        //查看用户10层内的团队业绩（充值记录总和）
        BigDecimal teamAchievement = memberTransactionDao.teamAchievement(longId);
        //查看用户团队业绩是否满足升级s1满足
        if(teamAchievement.compareTo(patternConf.getOneteamperformance())>=0){
            return true;
        }
        return false;
    }

    /**
     * 功能描述: 查看用户每层有几个S1
     * @param:
     * @return:
     */
    public int getEachLayerOne(Member member,PatternConf patternConf){
        int levelNum=0;
        List<Member> zhiTuiMembers = memberDao.zhiTuiMember(member.getId());
        for (Member m:zhiTuiMembers){
            //查看用户的团队信息
            List<Member> teamUsers = memberDao.teamUser(m.getId());
            for (Member teamUser:teamUsers){
                //查看会员级别信息
                Integer getlLevel = getlLevel(teamUser, patternConf);
                if(getlLevel==1){
                    levelNum=levelNum+1;
                    if(levelNum>=patternConf.getHavingone()){
                        return levelNum;
                    }
                }
            }
        }
        return levelNum;
    }

    /**
     * 功能描述: 查看用户每层有几个s2
     * @param:
     * @return:
     */
    public int getEachLayerTwo(Member member,PatternConf patternConf){
        int levelNum=0;
        List<Member> zhiTuiMembers = memberDao.zhiTuiMember(member.getId());
        for (Member m:zhiTuiMembers){
            int eachLayerOne = getEachLayerOne(m, patternConf);
            if(eachLayerOne>=patternConf.getHavingone()){
                levelNum=levelNum+1;
                if(levelNum>patternConf.getHavingtwo()){
                    return levelNum;
                }
            }
        }
        return levelNum;
    }

    /**
     * 功能描述: 查看用户每层有几个s3
     * @param:
     * @return:
     */
    public int getEachLayerThree(Member member,PatternConf patternConf){
        int levelNum=0;
        List<Member> zhiTuiMembers = memberDao.zhiTuiMember(member.getId());
        for (Member m:zhiTuiMembers){
            int eachLayerOne = getEachLayerTwo(m, patternConf);
            if(eachLayerOne>=patternConf.getHavingtwo()){
                levelNum=levelNum+1;
                if(levelNum>=patternConf.getHavingthree()){
                    return levelNum;
                }
            }
        }
        return levelNum;
    }

    /**
     * 功能描述: 查看用户当前是属于什么级别s1,s2,s3
     * @param:
     * @return:
     */
    public int getMemberLevel(Member member,PatternConf patternConf){
        int level=0;
        if(getEachLayerTwo(member, patternConf)>=patternConf.getHavingthree()){
            level=3;
        }else if(getEachLayerOne(member, patternConf)>=patternConf.getHavingone()){
            level=2;
        }else if(getlLevel(member,patternConf)==1){
            level=1;
        }
        return level;
    }



    /**
     * 功能描述: 查看会员当前级别信息是否是s1
     * @param:
     * @return:
     */
    public Integer getlLevel(Member member, PatternConf patternConf){
        //查看用户的直推会员信息
        List<Member> zhiTuiMembers = memberDao.zhiTuiMember(member.getId());
        //有效会员
        int effectiveMembership=0;
        for (Member zhiTuiMember:zhiTuiMembers){
            //判断用户是否是有效会员
            if(EffectiveMembership(zhiTuiMember.getId(),patternConf)){
                effectiveMembership = effectiveMembership+1;
            }
        }
        //查看用户团队10层内的用户ID
        List<Long> longId = memberDao.inLayer(member.getId(), member.getGeneration(), patternConf.getTeamlayer());
        BigDecimal teamAchievement=BigDecimal.ZERO;
        if(StringUtils.isEmpty(longId)||longId.size()==0){
            teamAchievement=BigDecimal.ZERO;
        }else {
            //查看用户10层内的团队业绩（充值记录总和）
            teamAchievement = memberTransactionDao.teamAchievement(longId);
        }

        //判断用户团队业绩是否满足升级s1及直推的有效会员是否满足s1
        if(teamAchievement.compareTo(patternConf.getOneteamperformance())>=0 && effectiveMembership>=patternConf.getEffectivemembership()){
           return 1;
        }
        return 0;
    }



    /**
     * 功能描述: 修改用户账户余额信息
     * @param:id 用户ID
     * @param:currency 操作的币种
     * @param:cost  操作的数量
     * @param:isout  操作的类型 1收入 0是支出
     * @return:
     */
    public void updateBalance(long id,String currency,BigDecimal cost,Integer isout,TransactionType type){
        if(cost.compareTo(BigDecimal.ZERO)!=1){
            return;
        }
        //获取用户对应币种的钱包信息
        MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId(currency, id);
        //余额明细
        MemberTransaction memberTransaction=new MemberTransaction();
        //操作金额
        BigDecimal costPrice=cost;
        if(isout==1){
            //增加余额
            memberWalletDao.increaseBalance(memberWallet.getId(),costPrice);
        }else if (isout==0){
            //减少余额
            memberWalletDao.decreaseBalance(memberWallet.getId(), costPrice);
            costPrice=costPrice.negate();
        }
        //插入明细
        memberTransaction.setAmount(costPrice);
        memberTransaction.setFee(BigDecimal.ZERO);
        memberTransaction.setMemberId(id);
        memberTransaction.setSymbol(currency);
        memberTransaction.setType(type);
        memberTransactionDao.save(memberTransaction);
    }




    /**
     * 功能描述: 查看用户是否是有效会员
     * @param:
     * @return:
     */
    public Boolean EffectiveMembership(long id, PatternConf patternConf){
        //获取用户所有的钱包信息
        List<MemberWallet> allByMemberId = memberWalletDao.findAllByMemberId(id);
        for (MemberWallet memberWallet:allByMemberId){
            //根据币种信息获取对应的转换USDT的汇率
            BigDecimal cnyRate = coinExchangeRate.getUsdRate(memberWallet.getCoin().getName());
            //算出可转换USDT的数量
            BigDecimal usdtNum = FunctionUtils.mul(FunctionUtils.add(memberWallet.getBalance(),memberWallet.getToReleased(),4), cnyRate, 4);
            //判断usdt的数量是否满足有效会员的条件
            if(usdtNum.compareTo(patternConf.getUsdtnummember())>=0){
                return true;
            }
        }
        return false;
    }

    /**
     * 功能描述: 获取模式的配置信息
     * @param:
     * @return:
     */
    public PatternConf getPatternConf(){
        return patternConfDao.getPatternConf();
    }


}

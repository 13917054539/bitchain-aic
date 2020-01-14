package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.MemberWalletDao;
import cn.ztuo.bitrade.entity.MemberTransaction;
import cn.ztuo.bitrade.entity.MemberWallet;
import cn.ztuo.bitrade.util.FunctionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * @Auther:路道
 * @Date:2019/8/6
 * @Description:cn.ztuo.bitrade.service
 * @version:1.0
 */
@Service
public class BIPBMathService {

    @Autowired
    private MemberWalletDao memberWalletDao;

    @Autowired
    private MemberTransactionDao memberTransactionDao;

    /**
     * 功能描述:修改用户余额
     * @param: walletId 钱包ID
     * @param:cost  操作的数量
     * @param:isout  操作的类型 1收入 0是支出
     * @param:type  操作的类型
     * @param:currency  操作的币种
     * @return:
     */
    @Transactional
    public String updateBalance(long walletId, BigDecimal cost, Integer isout, String currency ,TransactionType type){
        String result="";
        //获取钱包信息
        MemberWallet memberWallet = memberWalletDao.findById(walletId);
        if(isout==1){
            try {
                memberWalletDao.increaseBalance(memberWallet.getId(),cost);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(isout==0){
            BigDecimal price= FunctionUtils.sub(memberWallet.getBalance(),cost,2);
            if(price.compareTo(BigDecimal.ZERO)==-1){
                return "余额不足";
            }
            int i = memberWalletDao.decreaseBalance(walletId, cost);
            if(i==0){
                return "效验资金异常";
            }
            cost=cost.negate();
        }
        insertDetile(cost,memberWallet.getMemberId(),currency,type);
        return result;
    }


    /**
     * 功能描述: 修改用户待释放余额
     * @param:
     * @return:
     */
    @Transactional
    public String updateToReleased(Long walletId, BigDecimal cost, Integer isout, String currency , TransactionType type){
        String result="";
        //获取钱包信息
        MemberWallet memberWallet = memberWalletDao.findById(walletId);
        if(isout==1){
            memberWalletDao.increaseToReleased(walletId,cost);
        }else if(isout==0){
            BigDecimal price= FunctionUtils.sub(memberWallet.getToReleased(),cost,2);
            if(price.compareTo(BigDecimal.ZERO)==-1){
                return "余额不足";
            }
            int i = memberWalletDao.decreaseToReleased(walletId, cost);
            if(i==0){
                return "效验资金异常";
            }
            cost=cost.negate();
        }
        insertDetile(cost,memberWallet.getMemberId(),currency,type);
        return result;
    }

    /**
     * 功能描述: 修改用户待可转换数量
     * @param:
     * @return:
     */
    @Transactional
    public String updateConvertible(Long walletId, BigDecimal cost, Integer isout, String currency , TransactionType type){
        String result="";
        //获取钱包信息
        MemberWallet memberWallet = memberWalletDao.findById(walletId);
        if(isout==1){
            memberWalletDao.increaseConvertible(walletId,cost);
        }else if(isout==0){
            BigDecimal price= FunctionUtils.sub(memberWallet.getConvertible(),cost,2);
            if(price.compareTo(BigDecimal.ZERO)==-1){
                return "余额不足";
            }
            int i = memberWalletDao.decreaseConvertible(walletId, cost);
            if(i==0){
                return "效验资金异常";
            }
            cost=cost.negate();
        }
        insertDetile(cost,memberWallet.getMemberId(),currency,type);
        return result;
    }


    /**
     * 功能描述: 修改用户的额度
     * @param:
     * @return:
     */
    @Transactional
    public String updateConvertiblequota(Long walletId, BigDecimal cost, Integer isout, String currency , TransactionType type){
        String result="";
        //获取钱包信息
        MemberWallet memberWallet = memberWalletDao.findById(walletId);
        if(isout==1){
            memberWalletDao.increaseConvertiblequota(walletId,cost);
        }else if(isout==0){
            BigDecimal price= FunctionUtils.sub(memberWallet.getConvertiblequota(),cost,2);
            if(price.compareTo(BigDecimal.ZERO)==-1){
                return "余额不足";
            }
            int i = memberWalletDao.decreaseConvertiblequota(walletId, cost);
            if(i==0){
                return "效验资金异常";
            }
            cost=cost.negate();
        }
        insertDetile(cost,memberWallet.getMemberId(),currency,type);
        return result;
    }

    /**
     * 功能描述: 修改CTO的额度
     * @param:
     * @return:
     */
    @Transactional
    public String updateCtoquota(Long walletId, BigDecimal cost, Integer isout, String currency , TransactionType type){
        String result="";
        //获取钱包信息
        MemberWallet memberWallet = memberWalletDao.findById(walletId);
        if(isout==1){
            memberWalletDao.increaseCtoquota(walletId,cost);
        }else if(isout==0){
            BigDecimal price= FunctionUtils.sub(memberWallet.getCtoquota(),cost,2);
            if(price.compareTo(BigDecimal.ZERO)==-1){
                return "余额不足";
            }
            int i = memberWalletDao.decreaseCtoquota(walletId, cost);
            if(i==0){
                return "效验资金异常";
            }
            cost=cost.negate();
        }
        insertDetile(cost,memberWallet.getMemberId(),currency,type);
        return result;
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

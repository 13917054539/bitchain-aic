package cn.ztuo.bitrade.controller.member;

import cn.ztuo.bitrade.dao.RewardsToReleaseDao;
import cn.ztuo.bitrade.util.ToolUtil;
import com.alibaba.fastjson.JSONObject;
import com.querydsl.core.types.Predicate;
import cn.ztuo.bitrade.annotation.AccessLog;
import cn.ztuo.bitrade.constant.AdminModule;
import cn.ztuo.bitrade.constant.PageModel;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dto.MemberWalletDTO;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.es.ESUtils;
import cn.ztuo.bitrade.service.*;
import cn.ztuo.bitrade.util.DateUtil;
import cn.ztuo.bitrade.util.MessageResult;

import cn.ztuo.bitrade.controller.common.BaseAdminController;
import cn.ztuo.bitrade.model.screen.MemberWalletScreen;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("member/member-wallet")
@Slf4j
public class MemberWalletController extends BaseAdminController {

    @Autowired
    private MemberWalletService memberWalletService;

    @Autowired
    private MemberService memberService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private KafkaTemplate kafkaTemplate;
    @Autowired
    private MemberTransactionService memberTransactionService;
    @Autowired
    private LocaleMessageSourceService messageSource;
    @Autowired
    private RewardsToReleaseDao rewardsToReleaseDao;
    @Autowired
    private ESUtils esUtils;


    @RequiresPermissions("member:member-wallet:balance")
    @PostMapping("balance")
    @AccessLog(module = AdminModule.MEMBER, operation = "余额管理")
    public MessageResult getBalance(
            PageModel pageModel,
            MemberWalletScreen screen) {
        QMemberWallet qMemberWallet = QMemberWallet.memberWallet;
        QMember qMember = QMember.member;
        List<Predicate> criteria = new ArrayList<>();
        if (StringUtils.hasText(screen.getAccount())) {
            criteria.add(qMember.username.like("%" + screen.getAccount() + "%")
                    .or(qMember.mobilePhone.like(screen.getAccount() + "%"))
                    .or(qMember.email.like(screen.getAccount() + "%"))
                    .or(qMember.realName.like("%" + screen.getAccount() + "%")));
        }
        if (!StringUtils.isEmpty(screen.getWalletAddress())) {
            criteria.add(qMemberWallet.address.eq(screen.getWalletAddress()));
        }

        if (!StringUtils.isEmpty(screen.getUnit())) {
            criteria.add(qMemberWallet.coin.unit.eq(screen.getUnit()));
        }

        if (screen.getMaxAllBalance() != null) {
            criteria.add(qMemberWallet.balance.add(qMemberWallet.frozenBalance).loe(screen.getMaxAllBalance()));
        }

        if (screen.getMinAllBalance() != null) {
            criteria.add(qMemberWallet.balance.add(qMemberWallet.frozenBalance).goe(screen.getMinAllBalance()));
        }

        if (screen.getMaxBalance() != null) {
            criteria.add(qMemberWallet.balance.loe(screen.getMaxBalance()));
        }

        if (screen.getMinBalance() != null) {
            criteria.add(qMemberWallet.balance.goe(screen.getMinBalance()));
        }

        if (screen.getMaxFrozenBalance() != null) {
            criteria.add(qMemberWallet.frozenBalance.loe(screen.getMaxFrozenBalance()));
        }

        if (screen.getMinFrozenBalance() != null) {
            criteria.add(qMemberWallet.frozenBalance.goe(screen.getMinFrozenBalance()));
        }

        Page<MemberWalletDTO> page = memberWalletService.joinFind(criteria, qMember, qMemberWallet, pageModel);
        return success(messageSource.getMessage("SUCCESS"), page);
    }

    @RequiresPermissions("member:member-wallet:recharge")
    @PostMapping("recharge")
    @AccessLog(module = AdminModule.MEMBER, operation = "充币管理")
    public MessageResult recharge(
            @RequestParam("unit") String unit,
            @RequestParam("uid") Long uid,
            @RequestParam("amount") BigDecimal amount) {
        Coin coin = coinService.findByUnit(unit);
        if (coin == null) {
            return error("币种不存在");
        }
        MemberWallet memberWallet = memberWalletService.findByCoinAndMemberId(coin, uid);
        Assert.notNull(memberWallet, "wallet null");
        memberWallet.setBalance(memberWallet.getBalance().add(amount));

        MemberTransaction memberTransaction = new MemberTransaction();
        memberTransaction.setFee(BigDecimal.ZERO);
        memberTransaction.setAmount(amount);
        memberTransaction.setMemberId(memberWallet.getMemberId());
        memberTransaction.setSymbol(unit);
        memberTransaction.setType(TransactionType.ADMIN_RECHARGE);
        memberTransaction.setCreateTime(DateUtil.getCurrentDate());
        memberTransaction.setRealFee("0");
        memberTransaction.setDiscountFee("0");
        memberTransaction= memberTransactionService.save(memberTransaction);
        return success(messageSource.getMessage("SUCCESS"));
    }

    @RequiresPermissions("member:member-wallet:reset-address")
    @PostMapping("reset-address")
    @AccessLog(module = AdminModule.MEMBER, operation = "重置钱包地址")
    public MessageResult resetAddress(String unit, long uid) {
        Member member = memberService.findOne(uid);
        Assert.notNull(member, "member null");
        try {
            JSONObject json = new JSONObject();
            json.put("uid", member.getId());
            log.info("kafkaTemplate send : topic = {reset-member-address} , unit = {} , uid = {}", unit, json);
            kafkaTemplate.send("reset-member-address", unit, json.toJSONString());
            return MessageResult.success(messageSource.getMessage("SUCCESS"));
        } catch (Exception e) {
            return MessageResult.error(messageSource.getMessage("REQUEST_FAILED"));
        }
    }

    @RequiresPermissions("member:member-wallet:lock-wallet")
    @PostMapping("lock-wallet")
    @AccessLog(module = AdminModule.MEMBER, operation = "锁定钱包")
    public MessageResult lockWallet(Long uid, String unit) {
        if (memberWalletService.lockWallet(uid, unit)) {
            return success(messageSource.getMessage("SUCCESS"));
        } else {
            return error(500, messageSource.getMessage("REQUEST_FAILED"));
        }
    }

    @RequiresPermissions("member:member-wallet:unlock-wallet")
    @PostMapping("unlock-wallet")
    @AccessLog(module = AdminModule.MEMBER, operation = "解锁钱包")
    public MessageResult unlockWallet(Long uid, String unit) {
        if (memberWalletService.unlockWallet(uid, unit)) {
            return success(messageSource.getMessage("SUCCESS"));
        } else {
            return error(500, messageSource.getMessage("REQUEST_FAILED"));
        }
    }

    @RequiresPermissions("member:member-wallet:frozen-capital")
    @PostMapping("frozen-capital")
    @AccessLog(module = AdminModule.MEMBER, operation = "冻结资金")
    public MessageResult frozenCapital(Long uid, String unit, BigDecimal amount) {
        if(ToolUtil.isOneEmpty(uid,unit,amount)){
            return error("请求参数不完整");
        }
        MemberWallet memberWallet = memberWalletService.getMemberWalletByCoinAndMemberId(unit,uid);
        if(ToolUtil.isEmpty(memberWallet)){
            return error("钱包不存在");
        }
        if(memberWallet.getBalance().compareTo(amount) == -1){
            return error("余额不足"+amount+"冻结失败");
        }
        memberWallet.setBalance(memberWallet.getBalance().subtract(amount));
        memberWallet.setFrozenBalance(memberWallet.getFrozenBalance().add(amount));
        memberWalletService.save(memberWallet);
        MemberTransaction memberTransaction = new MemberTransaction();
        memberTransaction.setFlag(0);
        memberTransaction.setFee(BigDecimal.ZERO);
        memberTransaction.setAmount(amount.negate());
        memberTransaction.setCreateTime(new Date());
        memberTransaction.setType(TransactionType.FROZEN);
        memberTransaction.setSymbol(unit);
        memberTransaction.setMemberId(uid);
        memberTransaction.setRealFee("");
        memberTransaction.setDiscountFee("");
        memberTransactionService.save(memberTransaction);
        return success(messageSource.getMessage("SUCCESS"));
    }

    @RequiresPermissions("member:member-wallet:thaw-funds")
    @PostMapping("thaw-funds")
    @AccessLog(module = AdminModule.MEMBER, operation = "解冻资金")
    public MessageResult thawFunds(Long uid, String unit, BigDecimal amount) {
        if(ToolUtil.isOneEmpty(uid,unit,amount)){
            return error("请求参数不完整");
        }
        MemberWallet memberWallet = memberWalletService.getMemberWalletByCoinAndMemberId(unit,uid);
        if(ToolUtil.isEmpty(memberWallet)){
            return error("钱包不存在");
        }
        if(memberWallet.getFrozenBalance().compareTo(amount) == -1){
            return error("冻结余额不足"+amount+"解冻失败");
        }
        memberWallet.setBalance(memberWallet.getBalance().add(amount));
        memberWallet.setFrozenBalance(memberWallet.getFrozenBalance().subtract(amount));
        memberWalletService.save(memberWallet);
        MemberTransaction memberTransaction = new MemberTransaction();
        memberTransaction.setFlag(0);
        memberTransaction.setFee(BigDecimal.ZERO);
        memberTransaction.setAmount(amount);
        memberTransaction.setCreateTime(new Date());
        memberTransaction.setType(TransactionType.UNFREEZE);
        memberTransaction.setSymbol(unit);
        memberTransaction.setMemberId(uid);
        memberTransaction.setRealFee("");
        memberTransaction.setDiscountFee("");
        memberTransactionService.save(memberTransaction);
        return success(messageSource.getMessage("SUCCESS"));
    }

    @RequiresPermissions("member:member-wallet:add-assets-to-released")
    @PostMapping("add-assets-to-released")
    @AccessLog(module = AdminModule.MEMBER, operation = "充值待释放资产")
    public MessageResult addAssetsToReleased(Long uid, BigDecimal sumAmount,BigDecimal amount,String remarks) {
        if(ToolUtil.isOneEmpty(uid,sumAmount,amount,remarks)){
            return error("填写信息不完整");
        }
        MemberWallet memberWallet = memberWalletService.getMemberWalletByCoinAndMemberId("CAL",uid);
        if(ToolUtil.isEmpty(memberWallet)){
            return error("钱包不存在");
        }
        memberWallet.setToReleased(memberWallet.getToReleased().add(amount));
        memberWalletService.save(memberWallet);
        RewardsToRelease rewardsToRelease = new RewardsToRelease();
        rewardsToRelease.setQuantityToReleased(amount);
        rewardsToRelease.setReleasedQuantity(sumAmount.subtract(amount));
        rewardsToRelease.setTotalQuantity(sumAmount);
        rewardsToRelease.setSyboml("CAL");
        rewardsToRelease.setRemarks(remarks);
        rewardsToRelease.setReleaseDays(365);
        rewardsToRelease.setMemberId(uid);
        rewardsToRelease.setCreateTime(new Date());
        rewardsToReleaseDao.save(rewardsToRelease);
        return success(messageSource.getMessage("SUCCESS"));
    }
}

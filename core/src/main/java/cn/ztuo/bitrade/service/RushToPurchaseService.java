package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.EnumConfig;
import cn.ztuo.bitrade.constant.MemberLevelEnum;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.*;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.factory.ConstantFactory;
import cn.ztuo.bitrade.service.Base.BaseService;
import cn.ztuo.bitrade.util.Md5;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.util.ToolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;

import static cn.ztuo.bitrade.util.MessageResult.error;

/**
 * @author GS
 * @date 2017年12月18日
 */
@Service
@Slf4j
public class RushToPurchaseService extends BaseService{

    @Autowired
    private MemberWalletDao memberWalletDao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private MemberTransactionDao memberTransactionDao;
    @Autowired
    private SysConfigDao sysConfigDao;
    @Autowired
    private RewardsToReleaseDao rewardsToReleaseDao;
    @Autowired
    private LocaleMessageSourceService msService;
    @Autowired
    private ConstantFactory constantFactory;
    public MessageResult buy(AuthMember user, BigDecimal usdtAmount){
         
        return MessageResult.success("抢购成功");
    };
}

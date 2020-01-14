package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.MemberDao;
import cn.ztuo.bitrade.service.MemberTransactionService;
import com.alibaba.fastjson.JSONObject;

import cn.ztuo.bitrade.constant.PromotionLevel;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.entity.PromotionMember;
import cn.ztuo.bitrade.entity.PromotionRewardRecord;
import cn.ztuo.bitrade.entity.RewardRecord;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.service.RewardRecordService;
import cn.ztuo.bitrade.util.MessageResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推广
 *
 * @author GS
 * @date 2018年03月19日
 */
@RestController
@RequestMapping(value = "/promotion")
public class PromotionController extends BaseController{

    @Autowired
    private MemberService memberService;
    @Autowired
    private RewardRecordService rewardRecordService;
    @Autowired
    private MemberDao memberDao;


    @Autowired
    private MemberTransactionService memberTransactionService;


    /**
     * 推广记录查询
     *
     * @param user
     * @return
     */
//    @RequestMapping(value = "/record")
//    public MessageResult promotionRecord(@SessionAttribute(SESSION_MEMBER) AuthMember member) {
//        List<Member> list = memberService.findPromotionMember(member.getId());
//        List<PromotionMember> list1 = list.stream().map(x ->
//                PromotionMember.builder().createTime(x.getRegistrationTime())
//                        .level(PromotionLevel.ONE)
//                        .username(x.getUsername())
//                        .build()
//        ).collect(Collectors.toList());
//        if (list.size() > 0) {
//            list.stream().forEach(x -> {
//                if (x.getPromotionCode() != null) {
//                    list1.addAll(memberService.findPromotionMember(x.getId()).stream()
//                            .map(y ->
//                                    PromotionMember.builder().createTime(y.getRegistrationTime())
//                                            .level(PromotionLevel.TWO)
//                                            .username(y.getUsername())
//                                            .build()
//                            ).collect(Collectors.toList()));
//                }
//            });
//        }
//        MessageResult messageResult = MessageResult.success();
//        messageResult.setData(list1.stream().sorted((x, y) -> {
//            if (x.getCreateTime().after(y.getCreateTime())) {
//                return -1;
//            } else {
//                return 1;
//            }
//        }).collect(Collectors.toList()));
//        return messageResult;
//    }
    @RequestMapping(value = "/record")
    public MessageResult promotionRecord2(@SessionAttribute(SESSION_MEMBER) AuthMember user) {
        List<Map<String,Object>> data =new LinkedList<>();
       /* map.put("releaseReward",memberTransactionService.getSumReward(member.getId(), TransactionType.parseOrdinal(TransactionType.INTEREST_RATE_INCREASE_AWARD)));//释放
        map.put("levelReward",memberTransactionService.getSumReward(member.getId(),TransactionType.parseOrdinal(TransactionType.LEVEL_AWARD)));//级别
        map.put("directPush",memberService.getCountDirectpush(member.getId()));//直推人数
        map.put("team",memberService.getCountTeam(member.getId()));//团队总
        //只查找一级推荐人
        Page<Member> pageList = memberService.findPromotionMemberPage(pageNo-1, pageSize, member.getId());
        MessageResult messageResult = MessageResult.success();
        List<Member> list = pageList.getContent();
        List<PromotionMember> list1 = list.stream().map(x ->
                PromotionMember.builder().createTime(x.getRegistrationTime())
                        .level(PromotionLevel.ONE)
                        .username(x.getUsername())
                        .realNameStatus(x.getRealNameStatus().getOrdinal())
                        .build()
        ).collect(Collectors.toList());


        messageResult.setData(list1.stream().sorted((x, y) -> {
            if (x.getCreateTime().after(y.getCreateTime())) {
                return -1;
            } else {
                return 1;
            }
        }).collect(Collectors.toList()));

        messageResult.setTotalPage(pageList.getTotalPages() + "");
        messageResult.setTotalElement(pageList.getTotalElements() + "");
        map.put("messageResult",messageResult);*/
       Member member = memberDao.findOne(user.getId());
        List<Member> members = memberDao.findAllByPid(user.getId());
        for (Member m:members){
            /*if(m.getGeneration()-member.getGeneration()>5){
                continue;
            }*/
            Map<String ,Object> map = new HashMap<>();
            map.put("account",m.getId());
            map.put("inviterId",m.getInviterId());
            map.put("vipLevel",m.getVip());
            map.put("groupSize",memberDao.findAllByPid(m.getId()).size());
            map.put("algebra",m.getGeneration()-member.getGeneration());
            data.add(map);
        }
        return success(data);
    }


    /**
     * 推广奖励记录
     *
     * @param member
     * @return
     */
//    @RequestMapping(value = "/reward/record")
//    public MessageResult rewardRecord(@SessionAttribute(SESSION_MEMBER) AuthMember member) {
//        List<RewardRecord> list = rewardRecordService.queryRewardPromotionList(memberService.findOne(member.getId()));
//        MessageResult result = MessageResult.success();
//        result.setData(list.stream().map(x ->
//                PromotionRewardRecord.builder().amount(x.getAmount())
//                        .createTime(x.getCreateTime())
//                        .remark(x.getRemark())
//                        .symbol(x.getCoin().getUnit())
//                        .build()
//        ).collect(Collectors.toList()));
//        return result;
//    }


    /**
     * 只查询推荐奖励
     *
     * @param member
     * @return
     */
    @RequestMapping(value = "/reward/record")
    public MessageResult rewardRecord2(@SessionAttribute(SESSION_MEMBER) AuthMember member, @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<RewardRecord> pageList = rewardRecordService.queryRewardPromotionPage(pageNo, pageSize, memberService.findOne(member.getId()));
        MessageResult result = MessageResult.success();
        List<RewardRecord> list = pageList.getContent();
        result.setData(list.stream().map(x ->
                PromotionRewardRecord.builder().amount(x.getAmount())
                        .createTime(x.getCreateTime())
                        .remark(x.getRemark())
                        .symbol(x.getCoin().getUnit())
                        .build()
        ).collect(Collectors.toList()));

        result.setTotalPage(pageList.getTotalPages() + "");
        result.setTotalElement(pageList.getTotalElements() + "");
        return result;
    }

}

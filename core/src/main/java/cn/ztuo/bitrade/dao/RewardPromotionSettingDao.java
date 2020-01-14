package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.constant.PromotionRewardType;
import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.RewardPromotionSetting;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author GS
 * @date 2018年03月08日
 */
public interface RewardPromotionSettingDao extends BaseDao<RewardPromotionSetting> {
    RewardPromotionSetting findByStatusAndType(BooleanEnum booleanEnum, PromotionRewardType type);





}

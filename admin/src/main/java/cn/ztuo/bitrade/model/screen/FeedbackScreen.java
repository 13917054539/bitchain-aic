package cn.ztuo.bitrade.model.screen;

import cn.ztuo.bitrade.ability.ScreenAbility;
import cn.ztuo.bitrade.constant.AuditStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.Data;

import java.util.ArrayList;

@Data
public class FeedbackScreen implements ScreenAbility {
    private String memberName;
    private AuditStatus status;
    @Override
    public ArrayList<BooleanExpression> getBooleanExpressions() {
        return booleanExpressions;
    }

}

package cn.ztuo.bitrade.controller.qisDataReport;

import cn.ztuo.bitrade.controller.BaseController;
import cn.ztuo.bitrade.dao.MemberDao;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.MemberWalletDao;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.util.ToolUtil;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author GS
 * @date 2017年12月19日
 */


@Slf4j
@Controller
@RequestMapping("/data/toBeReleased")
public class ToBeReleasedController extends BaseController {

    @PersistenceContext
    private EntityManager em;



    /**
     * 查询待释放奖励
     */
    @ResponseBody
    @RequestMapping(value = "findToBeReleased")
    public MessageResult findToBeReleased(@RequestParam Map<String, Object> paramMap){

        if(paramMap.get("pageSize")!=null&&paramMap.get("pageNo")!=null){
            paramMap.put("pageNo",(Integer.valueOf(paramMap.get("pageNo").toString())-1)*Integer.valueOf(paramMap.get("pageSize").toString()));
            paramMap.put("pageSize",Integer.valueOf(paramMap.get("pageSize").toString()));
        }
        if(paramMap.get("pageNo")==null) {
            paramMap.put("pageNo", 0);
        }
        if(paramMap.get("pageSize")==null){
            paramMap.put("pageSize",10);
        }
        StringBuffer sql = new StringBuffer();
        sql.append("select rtr.member_id,rtr.total_quantity,rtr.release_days,rtr.released_quantity,rtr.quantity_to_released," +
                "rtr.syboml,rtr.remarks,rtr.create_time,m.inviter_id,m.vip,m.mobile_phone,m.real_name " +
                "from rewards_to_release rtr inner join member m on m.id = rtr.member_id where 1=1 ");
        if(ToolUtil.isNotEmpty(paramMap.get("memberId"))){
            sql.append(" and rtr.member_id =:memberId");
        }
        if(ToolUtil.isNotEmpty(paramMap.get("memberPhone"))){
            sql.append(" and m.mobile_phone =:memberPhone");
        }
        //总数量区间
        if(ToolUtil.isNotEmpty(paramMap.get("startTotalQuantity"))&& ToolUtil.isNotEmpty(paramMap.get("endTotalQuantity"))){
            sql.append(" and rtr.total_quantity between :startTotalQuantity and :endTotalQuantity");
        }else{
            if(ToolUtil.isNotEmpty(paramMap.get("startTotalQuantity"))){
                sql.append(" and rtr.total_quantity >= :startTotalQuantity");
            }else if(ToolUtil.isNotEmpty(paramMap.get("endTotalQuantity"))){
                sql.append(" and rtr.total_quantity <= :endTotalQuantity");
            }
        }
        //已释放数量区间
        if(ToolUtil.isNotEmpty(paramMap.get("startReleasedQuantity"))&& ToolUtil.isNotEmpty(paramMap.get("endReleasedQuantity"))){
            sql.append(" and rtr.released_quantity between :startReleasedQuantity and :endReleasedQuantity");
        }else{
            if(ToolUtil.isNotEmpty(paramMap.get("startReleasedQuantity"))){
                sql.append(" and rtr.released_quantity >= :startReleasedQuantity");
            }else if(ToolUtil.isNotEmpty(paramMap.get("endReleasedQuantity"))){
                sql.append(" and rtr.released_quantity <= :endReleasedQuantity");
            }
        }
        //待释放数量区间
        if(ToolUtil.isNotEmpty(paramMap.get("startQuantityToReleased"))&& ToolUtil.isNotEmpty(paramMap.get("endQuantityToReleased"))){
            sql.append(" and rtr.quantity_to_released between :startQuantityToReleased and :endQuantityToReleased");
        }else{
            if(ToolUtil.isNotEmpty(paramMap.get("startQuantityToReleased"))){
                sql.append(" and rtr.quantity_to_released >= :startQuantityToReleased");
            }else if(ToolUtil.isNotEmpty(paramMap.get("endQuantityToReleased"))){
                sql.append(" and rtr.quantity_to_released <= :endQuantityToReleased");
            }
        }
        //创建时间区间
        if(ToolUtil.isNotEmpty(paramMap.get("beginTime"))&& ToolUtil.isNotEmpty(paramMap.get("endTime"))){
            sql.append(" and DATE_FORMAT(rtr.create_time,'%Y-%m-%d %H:%i:%s') between DATE_FORMAT(:beginTime,'%Y-%m-%d %H:%i:%s') and DATE_FORMAT(:endTime,'%Y-%m-%d %H:%i:%s') ");
        }else{
            if(ToolUtil.isNotEmpty(paramMap.get("beginTime"))){
                sql.append(" and DATE_FORMAT(rtr.create_time,'%Y-%m-%d %H:%i:%s') >= DATE_FORMAT(:beginTime,'%Y-%m-%d %H:%i:%s')");
            }else if(ToolUtil.isNotEmpty(paramMap.get("endTime"))){
                sql.append(" and DATE_FORMAT(rtr.create_time,'%Y-%m-%d %H:%i:%s') <= DATE_FORMAT(:endTime,'%Y-%m-%d %H:%i:%s') ");
            }
        }
        if(ToolUtil.isNotEmpty(paramMap.get("remarks"))){
            sql.append(" and rtr.remarks like concat('%',:remarks,'%')");
        }

        sql.append(" order by rtr.create_time desc");
        Query query = em.createNativeQuery(sql.toString());
        query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        if(ToolUtil.isNotEmpty(paramMap.get("memberId"))){
            query.setParameter("memberId",paramMap.get("memberId"));
        }
        if(ToolUtil.isNotEmpty(paramMap.get("memberPhone"))){
            query.setParameter("memberPhone",paramMap.get("memberPhone"));
        }
        if(ToolUtil.isNotEmpty(paramMap.get("startTotalQuantity"))&& ToolUtil.isNotEmpty(paramMap.get("endTotalQuantity"))){
            query.setParameter("startTotalQuantity",paramMap.get("startTotalQuantity"));
            query.setParameter("endTotalQuantity",paramMap.get("endTotalQuantity"));
        }else{
            if(ToolUtil.isNotEmpty(paramMap.get("startTotalQuantity"))){
                query.setParameter("startTotalQuantity",paramMap.get("startTotalQuantity"));
            }else if(ToolUtil.isNotEmpty(paramMap.get("endTotalQuantity"))){
                query.setParameter("endTotalQuantity",paramMap.get("endTotalQuantity"));
            }
        }

        if(ToolUtil.isNotEmpty(paramMap.get("startReleasedQuantity"))&& ToolUtil.isNotEmpty(paramMap.get("endReleasedQuantity"))){
            query.setParameter("startReleasedQuantity",paramMap.get("startReleasedQuantity"));
            query.setParameter("endReleasedQuantity",paramMap.get("endReleasedQuantity"));
        }else{
            if(ToolUtil.isNotEmpty(paramMap.get("startReleasedQuantity"))){
                query.setParameter("startReleasedQuantity",paramMap.get("startReleasedQuantity"));
            }else if(ToolUtil.isNotEmpty(paramMap.get("endReleasedQuantity"))){
                query.setParameter("endReleasedQuantity",paramMap.get("endReleasedQuantity"));
            }
        }

        if(ToolUtil.isNotEmpty(paramMap.get("startQuantityToReleased"))&& ToolUtil.isNotEmpty(paramMap.get("endQuantityToReleased"))){
            query.setParameter("startQuantityToReleased",paramMap.get("startQuantityToReleased"));
            query.setParameter("endQuantityToReleased",paramMap.get("endQuantityToReleased"));
        }else{
            if(ToolUtil.isNotEmpty(paramMap.get("startQuantityToReleased"))){
                query.setParameter("startQuantityToReleased",paramMap.get("startQuantityToReleased"));
            }else if(ToolUtil.isNotEmpty(paramMap.get("endQuantityToReleased"))){
                query.setParameter("endQuantityToReleased",paramMap.get("endQuantityToReleased"));
            }
        }

        if(ToolUtil.isNotEmpty(paramMap.get("beginTime"))&& ToolUtil.isNotEmpty(paramMap.get("endTime"))){
            query.setParameter("beginTime",paramMap.get("beginTime"));
            query.setParameter("endTime",paramMap.get("endTime"));
        }else{
            if(ToolUtil.isNotEmpty(paramMap.get("beginTime"))){
                query.setParameter("beginTime",paramMap.get("beginTime"));
            }else if(ToolUtil.isNotEmpty(paramMap.get("endTime"))){
                query.setParameter("endTime",paramMap.get("endTime"));
            }
        }
        if(ToolUtil.isNotEmpty(paramMap.get("remarks"))){
            query.setParameter("remarks",paramMap.get("remarks"));
        }
        Map<String,Object> map = new HashMap<>();
        map.put("count",query.getResultList().size());
        query.setFirstResult(Integer.valueOf(paramMap.get("pageNo").toString()));
        query.setMaxResults(Integer.valueOf(paramMap.get("pageSize").toString()));
        List<Map<String,Object>> rewardForReleases = query.getResultList();
        map.put("rewardForReleases",rewardForReleases);
        return MessageResult.getSuccessInstance("获取成功",map);
    }



}

package cn.ztuo.bitrade.controller.qisDataReport;


import cn.ztuo.bitrade.controller.BaseController;
import cn.ztuo.bitrade.dao.MemberDao;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.MemberWalletDao;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.util.ToolUtil;
import com.mashape.unirest.http.utils.MapUtil;
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
import javax.tools.Tool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author GS
 * @date 2017年12月19日
 */


@Slf4j
@Controller
@RequestMapping("/data/performance")
public class PerformanceController extends BaseController {

    @PersistenceContext
    private EntityManager em;
    @Autowired
    private MemberWalletDao memberWalletDao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private MemberTransactionDao memberTransactionDao;


    /**
     * 根据id、手机号、时间区间查询业绩
     */
    @ResponseBody
    @RequestMapping(value = "findPerformance")
    public MessageResult findPerformance(@RequestParam Map<String, Object> paramMap){
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
        sql.append("select m1.id,m1.mobile_phone,m1.username,m1.inviter_id,(abs(ifnull(sum(mt.amount),0))+\n" +
                "(select abs(ifnull(sum(mt1.amount),0)) from member_transaction mt1 INNER JOIN member m on mt1.member_id = m.id INNER JOIN member m2 on FIND_IN_SET(m2.id,m.genes)  where mt1.type = 39 and mt1.member_id = m1.id )) as amount\n" +
                "from member_transaction mt INNER JOIN member m1 on mt.member_id = m1.id\n" +
                "where mt.type = 39 ");
        if(ToolUtil.isNotEmpty(paramMap.get("memberId"))){
            sql.append(" and FIND_IN_SET(:memberId,m.genes) ");
        }
        if(ToolUtil.isNotEmpty(paramMap.get("mobilePhone"))){
            sql.append(" and m1.mobile_phone = :mobilePhone ");
        }
        if(ToolUtil.isNotEmpty(paramMap.get("beginTime"))&& ToolUtil.isNotEmpty(paramMap.get("endTime"))){
            sql.append(" and DATE_FORMAT(mt.create_time,'%Y-%m-%d %H:%i:%s') between DATE_FORMAT(:beginTime,'%Y-%m-%d %H:%i:%s') and DATE_FORMAT(:endTime,'%Y-%m-%d %H:%i:%s') ");
        }else{
            if(ToolUtil.isNotEmpty(paramMap.get("beginTime"))){
                sql.append(" and DATE_FORMAT(mt.create_time,'%Y-%m-%d %H:%i:%s') >= DATE_FORMAT(:beginTime,'%Y-%m-%d %H:%i:%s')");
            }else if(ToolUtil.isNotEmpty(paramMap.get("endTime"))){
                sql.append(" and DATE_FORMAT(mt.create_time,'%Y-%m-%d %H:%i:%s') <= DATE_FORMAT(:endTime,'%Y-%m-%d %H:%i:%s') ");
            }
        }
        sql.append(" GROUP BY m1.id,m1.mobile_phone,m1.username,m1.inviter_id ");
        sql.append(" order by amount desc");
        Query query = em.createNativeQuery(sql.toString());
        query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        if(ToolUtil.isNotEmpty(paramMap.get("memberId"))){
            query.setParameter("memberId",paramMap.get("memberId"));
        }
        if(ToolUtil.isNotEmpty(paramMap.get("memberPhone"))){
            query.setParameter("memberPhone",paramMap.get("memberPhone"));
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
        Map<String,Object> map = new HashMap<>();
        map.put("count",query.getResultList().size());
        query.setFirstResult(Integer.valueOf(paramMap.get("pageNo").toString()));
        query.setMaxResults(Integer.valueOf(paramMap.get("pageSize").toString()));
        List<Map<String,Object>> performances = query.getResultList();
        map.put("performances",performances);
        return MessageResult.getSuccessInstance("获取成功",map);
    }


}

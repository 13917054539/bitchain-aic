package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.dao.PatternConfDao;
import cn.ztuo.bitrade.entity.*;
import com.mashape.unirest.http.utils.MapUtil;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;

import cn.ztuo.bitrade.constant.PageModel;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.pagination.Criteria;
import cn.ztuo.bitrade.pagination.PageResult;
import cn.ztuo.bitrade.pagination.Restrictions;
import cn.ztuo.bitrade.service.Base.BaseService;
import cn.ztuo.bitrade.util.DateUtil;
import cn.ztuo.bitrade.vo.MemberTransactionVO;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.*;

@Service
public class MemberTransactionService extends BaseService {
    @Autowired
    private MemberTransactionDao transactionDao;
    @Autowired
    private MemberWalletService walletService;
    @Autowired
    private PatternConfDao patternConfDao;

    @PersistenceContext
    private EntityManager em;

    /**
     * 条件查询对象 pageNo pageSize 同时传时分页
     *
     * @param booleanExpressionList
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Transactional(readOnly = true)
    public PageResult<MemberTransaction> queryWhereOrPage(List<BooleanExpression> booleanExpressionList, Integer pageNo, Integer pageSize) {
        List<MemberTransaction> list;
        JPAQuery<MemberTransaction> jpaQuery = queryFactory.selectFrom(QMemberTransaction.memberTransaction);
        if (booleanExpressionList != null) {
            jpaQuery.where(booleanExpressionList.toArray(new BooleanExpression[booleanExpressionList.size()]));
        }
        if (pageNo != null && pageSize != null) {
            list = jpaQuery.offset((pageNo - 1) * pageSize).limit(pageSize).fetch();
        } else {
            list = jpaQuery.fetch();
        }
        return new PageResult<>(list, jpaQuery.fetchCount());
    }

    /**
     * 保存交易记录
     *
     * @param transaction
     * @return
     */
    public MemberTransaction save(MemberTransaction transaction) {
        return transactionDao.saveAndFlush(transaction);
    }

    @Override
    public List<MemberTransaction> findAll() {
        return transactionDao.findAll();
    }


    public MemberTransaction findOne(Long id) {
        return transactionDao.findOne(id);
    }


    public List findAllByWhere(Date startTime, Date endTime, TransactionType type, Long memberId) {
        QMemberTransaction qMemberTransaction = QMemberTransaction.memberTransaction;
        List<BooleanExpression> booleanExpressionList = new ArrayList();
        if (startTime != null) {
            booleanExpressionList.add(qMemberTransaction.createTime.gt(startTime));
        }
        if (endTime != null) {
            booleanExpressionList.add(qMemberTransaction.createTime.lt(endTime));
        }
        if (type != null) {
            booleanExpressionList.add(qMemberTransaction.type.eq(type));
        }
        if (memberId != null) {
            booleanExpressionList.add(qMemberTransaction.memberId.eq(memberId));
        }
        return queryFactory.selectFrom(qMemberTransaction).
                where(booleanExpressionList.toArray(booleanExpressionList.toArray(new BooleanExpression[booleanExpressionList.size()])))
                .fetch();
    }

    public Page<MemberTransaction> queryByMember(Long uid, Integer pageNo, Integer pageSize, TransactionType type) {
        //排序方式 (需要倒序 这样    Criteria.sort("id","createTime.desc") ) //参数实体类为字段名
        Sort orders = Criteria.sortStatic("createTime.desc");
        //分页参数
        PageRequest pageRequest = new PageRequest(pageNo, pageSize, orders);
        //查询条件
        Criteria<MemberTransaction> specification = new Criteria<MemberTransaction>();
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("type", type, false));
        return transactionDao.findAll(specification, pageRequest);
    }

    public Page<MemberTransaction> queryByMember(Long uid, Integer pageNo, Integer pageSize,TransactionType type,String startDate,String endDate,String symbol) throws ParseException {
        //排序方式 (需要倒序 这样    Criteria.sort("id","createTime.desc") ) //参数实体类为字段名
        Sort orders = Criteria.sortStatic("createTime.desc");
        //分页参数
        PageRequest pageRequest = new PageRequest(pageNo-1, pageSize, orders);
        //查询条件
        Criteria<MemberTransaction> specification = new Criteria<MemberTransaction>();
        specification.add(Restrictions.eq("memberId", uid, false));
        if(type != null){
            specification.add(Restrictions.eq("type",type,false));
        }
        if(StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)){
            specification.add(Restrictions.gte("createTime",DateUtil.YYYY_MM_DD_MM_HH_SS.parse(startDate+" 00:00:00"),false));
            specification.add(Restrictions.lte("createTime",DateUtil.YYYY_MM_DD_MM_HH_SS.parse(endDate+" 23:59:59"),false));
        }
        if(StringUtils.isNotEmpty(symbol)){
            specification.add(Restrictions.eq("symbol",symbol,false));
        }
        Page<MemberTransaction> all = transactionDao.findAll(specification, pageRequest);
        List<MemberTransaction> content = all.getContent();
        for (MemberTransaction memberTransaction:content) {
            if(org.springframework.util.StringUtils.isEmpty(memberTransaction.getFee())){
                memberTransaction.setFee(BigDecimal.ZERO);
            }
            if(org.springframework.util.StringUtils.isEmpty(memberTransaction.getRealFee())){
                memberTransaction.setRealFee("0");
            }
            if(org.springframework.util.StringUtils.isEmpty(memberTransaction.getDiscountFee())){
                memberTransaction.setDiscountFee("0");
            }
        }
        return all;
    }

    public BigDecimal findMatchTransactionSum(Long uid,String symbol){
        List<TransactionType> types = new ArrayList<>();
        types.add(TransactionType.RECHARGE);
        types.add(TransactionType.EXCHANGE);
        types.add(TransactionType.ADMIN_RECHARGE);
        Map<String,Object> result = transactionDao.findMatchTransactionSum(uid,symbol,types);
        return new BigDecimal(result.get("amount").toString());
    }

    public List<MemberTransaction> findMatchTransaction(Long uid,String symbol){
        Sort orders = Criteria.sortStatic("createTime.asc");
        //查询条件
        Criteria<MemberTransaction> specification = new Criteria<MemberTransaction>();
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("flag",0,false));
        specification.add(Restrictions.eq("symbol",symbol,false));
        specification.add(Restrictions.gt("amount",0,false));
        List<TransactionType> types = new ArrayList<>();
        types.add(TransactionType.RECHARGE);
        types.add(TransactionType.EXCHANGE);
        types.add(TransactionType.ADMIN_RECHARGE);
        specification.add(Restrictions.in("type",types,false));
        List<MemberTransaction> transactions = transactionDao.findAll(specification,orders);
        return transactions;
    }

    @Transactional
    public void matchWallet(Long uid,String symbol,BigDecimal amount){
        List<MemberTransaction> transactions = findMatchTransaction(uid,symbol);
        BigDecimal deltaAmount = BigDecimal.ZERO;
        MemberWallet gccWallet = walletService.findByCoinUnitAndMemberId("GCC",uid);
        MemberWallet gcxWallet = walletService.findByCoinUnitAndMemberId("GCX",uid);

        for(MemberTransaction transaction:transactions){
            if(amount.compareTo(deltaAmount) > 0) {
                BigDecimal  amt = amount.subtract(deltaAmount).compareTo(transaction.getAmount()) > 0 ? transaction.getAmount() : amount.subtract(deltaAmount);
                deltaAmount = deltaAmount.add(amt);
                transaction.setFlag(1);
            }
            else {
                break;
            }
        }

        gccWallet.setBalance(gccWallet.getBalance().subtract(deltaAmount));
        gcxWallet.setBalance(gcxWallet.getBalance().add(deltaAmount));

        MemberTransaction transaction = new MemberTransaction();
        transaction.setAmount(deltaAmount);
        transaction.setSymbol(gcxWallet.getCoin().getUnit());
        transaction.setAddress(gcxWallet.getAddress());
        transaction.setMemberId(gcxWallet.getMemberId());
        transaction.setType(TransactionType.MATCH);
        transaction.setFee(BigDecimal.ZERO);
        //保存配对记录
        save(transaction);
        if(gccWallet.getBalance().compareTo(BigDecimal.ZERO) < 0){
            gccWallet.setBalance(BigDecimal.ZERO);
        }
    }

    public boolean isOverMatchLimit(String day,double limit) throws Exception {
        BigDecimal totalAmount;
        Date date1 = DateUtil.YYYY_MM_DD_MM_HH_SS.parse(day+" 00:00:00");
        Date date2 = DateUtil.YYYY_MM_DD_MM_HH_SS.parse(day+" 23:59:59");
        Map<String,Object> result = transactionDao.findMatchTransactionSum("GCX",TransactionType.MATCH,date1,date2);
        if(result !=null && result.containsKey("amount")) {
            totalAmount = new BigDecimal(result.get("amount").toString());
        }
        else{
            totalAmount = BigDecimal.ZERO;
        }
        System.out.println("totalAmount:"+totalAmount);
        return totalAmount.doubleValue() >= limit;
    }

    public BigDecimal findMemberDailyMatch(Long uid,String day) throws Exception {
        Date date1 = DateUtil.YYYY_MM_DD_MM_HH_SS.parse(day+" 00:00:00");
        Date date2 = DateUtil.YYYY_MM_DD_MM_HH_SS.parse(day+" 23:59:59");
        Map<String,Object> result = transactionDao.findMatchTransactionSum(uid,"GCX",TransactionType.MATCH,date1,date2);
        if(result !=null && result.containsKey("amount")) {
            return new BigDecimal(result.get("amount").toString());
        }
        else {
            return BigDecimal.ZERO;
        }
    }

    public Page<MemberTransactionVO> joinFind(List<Predicate> predicates, PageModel pageModel){
        List<OrderSpecifier> orderSpecifiers = pageModel.getOrderSpecifiers() ;
        JPAQuery<MemberTransactionVO> query = queryFactory.select(Projections.fields(MemberTransactionVO.class,
                QMemberTransaction.memberTransaction.address,
                QMemberTransaction.memberTransaction.amount,
                QMemberTransaction.memberTransaction.createTime.as("createTime"),
                QMemberTransaction.memberTransaction.fee,
                QMemberTransaction.memberTransaction.flag,
                QMemberTransaction.memberTransaction.id.as("id"),
                QMemberTransaction.memberTransaction.symbol,
                QMemberTransaction.memberTransaction.type,
                QMember.member.username.as("memberUsername"),
                QMember.member.mobilePhone.as("phone"),
                QMember.member.email,
                QMember.member.realName.as("memberRealName"),
                QMember.member.id.as("memberId")))
                .from(QMemberTransaction.memberTransaction, QMember.member);
        predicates.add(QMemberTransaction.memberTransaction.memberId.eq(QMember.member.id));
        query.where(predicates.toArray(new BooleanExpression[predicates.size()]));
        query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[orderSpecifiers.size()]));
        List<MemberTransactionVO> list = query.offset((pageModel.getPageNo()-1)*pageModel.getPageSize()).limit(pageModel.getPageSize()).fetch();
        long total = query.fetchCount();
        return new PageImpl<>(list, pageModel.getPageable(), total);
    }

    /**
     * 功能描述: 获取用户的提现手续费
     * @param:
     * @return:
     */
    public BigDecimal getPaymentFee(long id,String symbol){
        //查看用户第一次的充值记录
        MemberTransaction paymentFee = transactionDao.getPaymentFee(id,symbol);
        if(org.springframework.util.StringUtils.isEmpty(paymentFee)){
            return BigDecimal.ZERO;
        }else {
            Date createTime = paymentFee.getCreateTime();
            PatternConf patternConf = patternConfDao.getPatternConf();
            int i = DateUtil.differentDays(createTime, new Date());
            if (i>=30){
                return patternConf.getAfterpaymentfee();
            }else {
                return patternConf.getWithinpaymentfee();
            }
        }
    }

    /**
     * 功能描述:
     * @param:
     * @return:
     */
    public BigDecimal getWithdrawalprotie(long id){
        PatternConf patternConf = patternConfDao.getPatternConf();
        return patternConf.getWithdrawalprotie();
    }

    /**
     * 功能描述：分页获取会员交易记录 条件为会员id以及类型
     * @param paramMap
     * @return
     */
    public List<MemberTransaction> getMemberTransactionByPage(Map<String,Object> paramMap){
        if(paramMap.get("pageSize")!=null&&paramMap.get("pageNo")!=null){
            paramMap.put("pageNo",((int)paramMap.get("pageNo")-1)*(int)paramMap.get("pageSize"));
            paramMap.put("pageSize",(int)paramMap.get("pageSize"));
        }
        if(paramMap.get("pageNo")==null) {
            paramMap.put("pageNo", 0);
        }
        if(paramMap.get("pageSize")==null){
            paramMap.put("pageSize",10);
        }
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT * FROM member_transaction where 1=1 ");
        if(paramMap.get("memberId")!=null){
            sql.append(" and member_id =:memberId");
        }
        if(paramMap.get("type")!=null){
            sql.append(" and type =:type");
        }else{
            sql.append(" and type IN(15,16,17,18)");
        }
        if(paramMap.get("date")!=null){
            sql.append(" and DATE_FORMAT( create_time, '%Y-%m-%d' ) =:date");
        }
        sql.append(" order by create_time desc");
        Query query =  em.createNativeQuery(sql.toString());
        query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        if(paramMap.get("memberId")!=null){
            query.setParameter("memberId",paramMap.get("memberId"));
        }

        if(paramMap.get("type")!=null){
            query.setParameter("type",paramMap.get("type"));
        }
        if(paramMap.get("date")!=null){
            query.setParameter("date",paramMap.get("date"));
        }
        Map<String,Object> map = new HashMap<>();
        map.put("count",query.getResultList().size());
        query.setFirstResult((int)paramMap.get("pageNo"));
        query.setMaxResults((int)paramMap.get("pageSize"));
        List<MemberTransaction> memberHashrateRecords = query.getResultList();

        return memberHashrateRecords;
    }

    /**
     * 功能描述：获取总行数
     * @param paramMap
     * @return
     */
    public int getMemberTransactionCount(Map<String,Object> paramMap){
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT COUNT(*)  AS count FROM member_transaction where 1=1 ");
        if(paramMap.get("memberId")!=null){
            sql.append(" and member_id =:memberId");
        }
        if(paramMap.get("type")!=null){
            sql.append(" and type =:type");
        }else{
            sql.append(" and type IN(15,16,17,18)");
        }
        if(paramMap.get("date")!=null){
            sql.append(" and DATE_FORMAT( create_time, '%Y-%m-%d' ) =:date");
        }
        Query query =  em.createNativeQuery(sql.toString());
        query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        if(paramMap.get("memberId")!=null){
            query.setParameter("memberId",paramMap.get("memberId"));
        }

        if(paramMap.get("type")!=null){
            query.setParameter("type",paramMap.get("type"));
        }
        if(paramMap.get("date")!=null){
            query.setParameter("date",paramMap.get("date"));
        }
        Map<String,Object> map = new HashMap<>();
        map.put("count",query.getResultList().size());
        Map<String,Object> mapResult= (Map<String,Object>)query.getSingleResult();
        BigInteger total=(BigInteger)mapResult.get("count");
        return total.intValue();
    }

    /**
     * 获取交易金额总数
     * @param paramMap
     * @return
     */
    public BigDecimal getAmountByType(Map<String,Object> paramMap){

        StringBuffer sql = new StringBuffer();
        sql.append("SELECT ROUND(IFNULL(SUM(amount),0.00),2) AS amountsum  FROM member_transaction  where 1=1 ");
        //动态sql判断
        if(paramMap.get("memberId")!=null){
            sql.append(" and member_id =:memberId");
        }
        if(paramMap.get("type")!=null){
            sql.append(" and type =:type");
        }else{
            sql.append(" and type IN(15,16,17,18)");
        }
        if(paramMap.get("date")!=null&&!"".equals(paramMap.get("date"))){
            sql.append(" AND DATE_FORMAT( create_time, '%Y-%m-%d' ) =:date");
        }
        if (paramMap.get("toDay")!=null){
            sql.append(" AND DATE_FORMAT( create_time, '%Y-%m-%d' )= CURDATE() ");
        }
        Query query =  em.createNativeQuery(sql.toString());
        query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        if(paramMap.get("memberId")!=null){
            query.setParameter("memberId",paramMap.get("memberId"));
        }

        if(paramMap.get("type")!=null){
            query.setParameter("type",paramMap.get("type"));
        }
        if(paramMap.get("date")!=null){
            query.setParameter("date",paramMap.get("date"));
        }

        Map<String,Object> sum= (Map<String,Object>)query.getSingleResult();

        return (BigDecimal) sum.get("amountsum");
    }

    /***
     * 功能描述:统计奖励
     * @param memberId
     * @param type
     * @return
     */
    public BigDecimal getSumReward(long memberId,int type){
        return transactionDao.getSumReward(memberId,type);
    }



}

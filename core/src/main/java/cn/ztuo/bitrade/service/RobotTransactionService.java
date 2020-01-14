package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.PageModel;
import cn.ztuo.bitrade.constant.TransactionType;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.PatternConfDao;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.pagination.Criteria;
import cn.ztuo.bitrade.pagination.PageResult;
import cn.ztuo.bitrade.pagination.Restrictions;
import cn.ztuo.bitrade.service.Base.BaseService;
import cn.ztuo.bitrade.util.DateUtil;
import cn.ztuo.bitrade.vo.MemberTransactionVO;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class RobotTransactionService extends BaseService {
}

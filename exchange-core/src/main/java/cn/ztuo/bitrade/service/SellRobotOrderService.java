package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.constant.CommonStatus;
import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.dao.*;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.factory.ConstantFactory;
import cn.ztuo.bitrade.util.MessageResult;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 十三
 * @date 2019年11月5日02:35:25
 */
@Slf4j
@Service
public class SellRobotOrderService extends MessageResult {

    @PersistenceContext
    private EntityManager em;
    @Autowired
    private RobotOrderDao robotOrderDao;
    @Autowired
    private MemberWalletDao memberWalletDao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private SysConfigDao sysConfigDao;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MemberTransactionDao memberTransactionDao;





}


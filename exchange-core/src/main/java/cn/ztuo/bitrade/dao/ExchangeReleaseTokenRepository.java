package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.entity.ExchangeReleaseToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface ExchangeReleaseTokenRepository extends MongoRepository<ExchangeReleaseToken,String>{

    List<ExchangeReleaseToken> findByReleaseTimeBetweenAndMemberIdIs(long startTime,long endTime,long memberId);
}

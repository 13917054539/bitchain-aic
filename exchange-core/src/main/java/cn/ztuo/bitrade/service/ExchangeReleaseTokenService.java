package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.dao.ExchangeReleaseTokenRepository;
import cn.ztuo.bitrade.entity.ExchangeReleaseToken;
import cn.ztuo.bitrade.service.Base.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ExchangeReleaseTokenService extends BaseService {

    @Autowired
    private ExchangeReleaseTokenRepository releaseTokenRepository;

    public void save(ExchangeReleaseToken exchangeReleaseToken){
        releaseTokenRepository.insert(exchangeReleaseToken);
    }

    public List<ExchangeReleaseToken> findByReleaseTimeBetweenAndMemberIdIs(long startTime, long endTime, long memberId){
        return releaseTokenRepository.findByReleaseTimeBetweenAndMemberIdIs(startTime,endTime,memberId);
    }
}

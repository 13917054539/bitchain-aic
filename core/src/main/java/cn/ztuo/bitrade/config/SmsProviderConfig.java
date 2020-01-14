package cn.ztuo.bitrade.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.ztuo.bitrade.vendor.provider.SMSProvider;
import cn.ztuo.bitrade.vendor.provider.support.ChuangRuiSMSProvider;
import cn.ztuo.bitrade.vendor.provider.support.EmaySMSProvider;
import cn.ztuo.bitrade.vendor.provider.support.HuaXinSMSProvider;

@Slf4j
@Configuration
public class SmsProviderConfig {

    @Value("${sms.gateway:}")
    private String gateway;
    @Value("${sms.username:}")
    private String username;
    @Value("${sms.password:}")
    private String password;
    @Value("${sms.sign:}")
    private String sign;
    @Value("${sms.internationalGateway:}")
    private String internationalGateway;
    @Value("${sms.internationalUsername:}")
    private String internationalUsername;
    @Value("${sms.internationalPassword:}")
    private String internationalPassword;
    @Value("${access.key.id:}")
    private String accessKey;
    @Value("${access.key.secret:}")
    private String accessSecret;


    @Bean
    public SMSProvider getSMSProvider(@Value("${sms.driver}") String driverName) {
        return new HuaXinSMSProvider(gateway, username, password,internationalGateway,internationalUsername,internationalPassword,sign);
        /*System.out.println("66666666666666666666666666");
        System.out.println(driverName);
        if (driverName.equalsIgnoreCase(ChuangRuiSMSProvider.getName())) {
            return new ChuangRuiSMSProvider(gateway, username, password, sign,accessKey,accessSecret);
        } else if (driverName.equalsIgnoreCase(EmaySMSProvider.getName())) {
            return new EmaySMSProvider(gateway, username, password);
        }else if (driverName.equalsIgnoreCase(HuaXinSMSProvider.getName())) {
            return new HuaXinSMSProvider(gateway, username, password,internationalGateway,internationalUsername,internationalPassword,sign);
        }  else {
            return null;
        }*/
    }
}

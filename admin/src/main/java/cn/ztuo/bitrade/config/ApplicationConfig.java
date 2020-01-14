package cn.ztuo.bitrade.config;

import cn.ztuo.bitrade.ext.OrdinalToEnumConverterFactory;

import cn.ztuo.bitrade.interceptor.LogInterceptor;
import cn.ztuo.bitrade.interceptor.OutExcelInterceptor;
import cn.ztuo.bitrade.interceptor.SessionInterceptor;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Administrator
 * 自定义配置一种方式
 */
@Configuration
public class ApplicationConfig extends WebMvcConfigurerAdapter {

    /**
     * 国际化
     * @return
     */
    @Bean(name = "messageSource")
    public ResourceBundleMessageSource getMessageSource() {
        ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
        resourceBundleMessageSource.setDefaultEncoding("UTF-8");
        resourceBundleMessageSource.setBasenames("i18n/messages", "i18n/ValidationMessages");
        resourceBundleMessageSource.setCacheSeconds(3600);
        return resourceBundleMessageSource;
    }

    /**
     * 跨域过滤
     * @return
     */
    @Bean
    public FilterRegistrationBean corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");//允许所有的请求域名访问我们的跨域资源
        config.setAllowCredentials(true);//认证
        config.addAllowedHeader("*");//允许所有的请求header访问，可以自定义设置任意请求头信息
        config.addAllowedMethod("*");//允许所有的请求方法访问该跨域资源服务器
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }
    /** 静态资源处理 **/
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/asset/**").addResourceLocations("classpath:/asset/");
        super.addResourceHandlers(registry);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new OrdinalToEnumConverterFactory());
        super.addFormatters(registry);
    }
    /** 添加拦截器 **/
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionInterceptor()).addPathPatterns("/**")
                .excludePathPatterns("/code/sms-provider/**","/captcha","/system/employee/sign/in",
                        "/system/employee/check","/system/employee/logout");
        registry.addInterceptor(new LogInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(new OutExcelInterceptor()).addPathPatterns("/**/out-excel");
        super.addInterceptors(registry);
    }

}

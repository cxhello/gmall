package com.cxhello.gmall.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author CaiXiaoHui
 * @create 2019-07-13 20:30
 */
@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 加入拦截器
     * @param registry
     */
    public void addInterceptors(InterceptorRegistry registry) {
        //将写好的拦截器配置到拦截器中心
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
        //
        super.addInterceptors(registry);
    }
}

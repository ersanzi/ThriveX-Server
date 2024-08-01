package liuyuyang.net.config;

import liuyuyang.net.interceptor.JwtTokenAdminInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 自定义的拦截器对象
    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册自定义拦截器对象
        registry.addInterceptor(jwtTokenAdminInterceptor)
                // 设置拦截的请求路径（ /** 表示拦截所有请求）
                // 拦截所有/admin的路由，并排除login
                // .addPathPatterns("/admin/**")
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login");
    }
}
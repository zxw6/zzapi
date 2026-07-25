package com.zxw.config;

import com.zxw.common.security.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
/**
 * Web MVC 配置类。
 * 负责注册后台鉴权拦截器、跨域策略以及首页跳转规则。
 */
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final GatewayCorsProperties gatewayCorsProperties;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor,
                        GatewayCorsProperties gatewayCorsProperties) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.gatewayCorsProperties = gatewayCorsProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 后台接口默认都需要登录，登录和健康检查接口除外
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/auth/login/captcha",
                        "/admin/auth/login",
                        "/admin/auth/register/code",
                        "/admin/auth/register",
                        "/admin/auth/password/reset/code",
                        "/admin/auth/password/reset",
                        "/admin/system/health",
                        "/admin/system/sse-protocol"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 网关跨域配置集中从配置对象中读取
        var cors = registry.addMapping("/**")
                .allowedOriginPatterns(gatewayCorsProperties.allowedOriginPatterns().toArray(String[]::new))
                .allowedMethods(gatewayCorsProperties.allowedMethods().toArray(String[]::new))
                .allowedHeaders(gatewayCorsProperties.allowedHeaders().toArray(String[]::new))
                .allowCredentials(gatewayCorsProperties.allowCredentials())
                .maxAge(gatewayCorsProperties.maxAge());
        if (!gatewayCorsProperties.exposedHeaders().isEmpty()) {
            cors.exposedHeaders(gatewayCorsProperties.exposedHeaders().toArray(String[]::new));
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String generatedImagesLocation = Path.of("generated-images").toAbsolutePath().normalize().toUri().toString();
        String tempGeneratedImagesLocation = Path.of(System.getProperty("java.io.tmpdir"), "zzapi-generated-images")
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler("/generated-images/**")
                .addResourceLocations(generatedImagesLocation, tempGeneratedImagesLocation);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 访问根路径时统一跳转到控制台首页
        registry.addRedirectViewController("/", "/console/index.html");
        registry.addRedirectViewController("/console", "/console/index.html");
        registry.addRedirectViewController("/console/", "/console/index.html");
    }
}

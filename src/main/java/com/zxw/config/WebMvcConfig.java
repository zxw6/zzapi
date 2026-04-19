package com.zxw.config;

import com.zxw.common.security.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
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
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/auth/login",
                        "/admin/auth/register",
                        "/admin/system/health",
                        "/admin/system/sse-protocol"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
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
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/console/index.html");
        registry.addRedirectViewController("/console", "/console/index.html");
        registry.addRedirectViewController("/console/", "/console/index.html");
    }
}

package com.zxw;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@EnableScheduling
@MapperScan("com.zxw.persistence.mapper")
@SpringBootApplication
/**
 * Spring Boot 应用启动入口。
 * 负责加载配置、定时任务和 MyBatis Mapper。
 */
public class TestApplication {

    /**
     * 启动整个项目。
     */
    public static void main(String[] args) {
        // 交给 Spring Boot 完成组件装配、配置绑定和服务启动
        SpringApplication.run(TestApplication.class, args);
    }
}

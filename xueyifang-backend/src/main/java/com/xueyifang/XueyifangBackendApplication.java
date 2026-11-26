package com.xueyifang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 学艺坊后端应用启动类
 *
 * @author xueyifang
 */
@SpringBootApplication
@MapperScan("com.xueyifang.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableScheduling
public class XueyifangBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(XueyifangBackendApplication.class, args);




        System.out.println("========================================");
        System.out.println("学艺坊后端启动成功！");
        System.out.println("API文档地址: http://localhost:8080/api/doc.html");
        System.out.println("========================================");
    }
}


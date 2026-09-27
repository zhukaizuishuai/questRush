package com.learn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 在线刷题学习平台后端启动类
 * 技术栈：Spring Boot 3.2 / Sa-Token / MyBatis-Plus / MySQL 8 / EasyExcel / easy-captcha
 */
@SpringBootApplication
public class LearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearnApplication.class, args);
    }
}

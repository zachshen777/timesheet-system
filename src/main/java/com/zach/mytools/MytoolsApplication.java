package com.zach.mytools;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@MapperScan("com.zach.mytools.mapper")
public class MytoolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MytoolsApplication.class, args);
        log.info("我的工具系统启动成功！");
        log.info("访问地址: http://localhost:5173");
    }
}

package com.grow.lyy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.grow.lyy.**.mapper")
public class LyyApplication {

	public static void main(String[] args) {

		SpringApplication.run(
				LyyApplication.class,
				args
		);

	}

}
package com.grow.lyy.common.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates the per-user desktop data/config directories before the H2 DataSource
 * is initialized.
 */
@Configuration(proxyBeanMethods = false)
@Profile("desktop")
public class DesktopDataDirectoryConfig {

    @Bean
    public static BeanFactoryPostProcessor desktopDataDirectories() {
        return beanFactory -> createDirectories(beanFactory);
    }

    private static void createDirectories(ConfigurableListableBeanFactory beanFactory) {
        Path base = Path.of(System.getProperty("user.home"), ".growth-garden");
        try {
            Files.createDirectories(base.resolve("data"));
            Files.createDirectories(base.resolve("config"));
            Files.createDirectories(base.resolve("logs"));
        } catch (IOException e) {
            throw new BeanDefinitionStoreException(
                    "无法创建桌面数据目录: " + base,
                    e
            );
        }
    }
}

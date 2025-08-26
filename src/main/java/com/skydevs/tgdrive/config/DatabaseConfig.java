package com.skydevs.tgdrive.config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;

@Configuration
@Slf4j
public class DatabaseConfig implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 在所有Bean创建之前执行，确保在Flyway初始化之前创建数据库目录
        String databaseUrl = environment.getProperty("spring.datasource.url");
        if (databaseUrl != null) {
            // 从数据库URL中提取路径
            String dbPath = databaseUrl.replace("jdbc:sqlite:", "");
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();

            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    log.info("数据库父目录创建成功: {}", parentDir.getAbsolutePath());
                } else {
                    log.error("数据库父目录创建失败: {}", parentDir.getAbsolutePath());
                }
            } else if (parentDir != null) {
                log.info("数据库父目录已存在: {}", parentDir.getAbsolutePath());
            }
        }else{
            throw new RuntimeException("数据库URL未配置");
        }
    }
}

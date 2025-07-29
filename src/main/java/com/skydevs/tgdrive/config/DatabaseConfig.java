package com.skydevs.tgdrive.config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @PostConstruct
    public void init() {
        // 从数据库URL中提取路径
        String dbPath = databaseUrl.replace("jdbc:sqlite:", "");
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                log.info("数据库父目录创建失败");
            }
        }
    }
}

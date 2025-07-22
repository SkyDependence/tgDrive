package com.skydevs.tgdrive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication(scanBasePackages = {"com.skydevs.tgdrive", "com.stanley"})
@EnableAsync
@EnableScheduling
@Transactional
public class TgDriveApplication {
    public static void main(String[] args) {
       SpringApplication.run(TgDriveApplication.class, args);
    }

}

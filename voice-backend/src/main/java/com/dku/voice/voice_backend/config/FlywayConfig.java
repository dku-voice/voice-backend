package com.dku.voice.voice_backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    /**
     * entityManagerFactory Bean이 flyway Bean에 의존하도록 설정
     * Bean 이름으로 직접 지정하여 실행 순서 보장
     */
    @Bean
    public static BeanFactoryPostProcessor dependsOnPostProcessor() {
        return beanFactory -> {
            var beanDef = beanFactory.getBeanDefinition("entityManagerFactory");
            beanDef.setDependsOn("flyway");
        };
    }

    /**
     * Flyway Bean 직접 등록
     * initMethod = "migrate" → Bean 생성 시 자동으로 migrate() 실행
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}
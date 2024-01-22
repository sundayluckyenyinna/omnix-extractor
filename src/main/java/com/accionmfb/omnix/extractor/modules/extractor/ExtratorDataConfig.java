package com.accionmfb.omnix.extractor.modules.extractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(value = DatasourceProps.class)
public class ExtratorDataConfig {

    private final DatasourceProps props;

    @Bean
    public DataSource dataSource(){
        System.out.println(props.toString());
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setPassword(props.getPassword());
        dataSource.setUsername(props.getUsername());
        dataSource.setUrl(props.getUrl());
        dataSource.setDriverClassName(props.getDriverClassName());
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(){
        return new JdbcTemplate(dataSource());
    }

}

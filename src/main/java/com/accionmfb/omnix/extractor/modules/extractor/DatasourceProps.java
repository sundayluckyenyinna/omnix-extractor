package com.accionmfb.omnix.extractor.modules.extractor;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ToString
@ConfigurationProperties(prefix = "spring.datasource")
public class DatasourceProps {

    private String username;
    private String password;
    private String url;
    private String driverClassName;
}

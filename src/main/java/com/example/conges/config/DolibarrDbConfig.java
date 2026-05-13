package com.example.conges.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DolibarrDbConfig {

    @Bean
    @ConditionalOnProperty(name = "dolibarr.db.enabled", havingValue = "true", matchIfMissing = true)
    @Qualifier("dolibarrDataSource")
    public DataSource dolibarrDataSource(
            @Value("${dolibarr.db.host:}") String host,
            @Value("${dolibarr.db.port:3306}") int port,
            @Value("${dolibarr.db.name:}") String dbName,
            @Value("${dolibarr.db.user:}") String user,
            @Value("${dolibarr.db.password:}") String password
    ) {
        // Empty host/name means "not configured" -> we still create a DS to fail fast if used.
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(url)
                .username(user)
                .password(password)
                .build();
    }

    @Bean
    @Qualifier("dolibarrJdbcTemplate")
    public JdbcTemplate dolibarrJdbcTemplate(@Qualifier("dolibarrDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}


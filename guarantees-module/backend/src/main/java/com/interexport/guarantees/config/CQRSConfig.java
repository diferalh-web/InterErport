package com.interexport.guarantees.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * CQRS Configuration for dual database setup
 * Command DB (Write) and Query DB (Read)
 * 
 * This configuration is ONLY active when the 'cqrs' Spring profile is enabled.
 * For single database mode, use the default Spring Boot auto-configuration.
 */
@Configuration
@Profile("cqrs")
@EnableJpaRepositories(
    basePackages = {
        "com.interexport.guarantees.repository",
        "com.interexport.guarantees.cqrs.command"
    },
    entityManagerFactoryRef = "commandEntityManagerFactory",
    transactionManagerRef = "commandTransactionManager"
)
public class CQRSConfig {
    
    @Value("${spring.datasource.url}")
    private String commandUrl;
    
    @Value("${spring.datasource.username}")
    private String commandUsername;
    
    @Value("${spring.datasource.password}")
    private String commandPassword;
    
    @Value("${spring.datasource.query.url}")
    private String queryUrl;
    
    @Value("${spring.datasource.query.username}")
    private String queryUsername;
    
    @Value("${spring.datasource.query.password}")
    private String queryPassword;
    
    /**
     * Command Database (Write Side) - Primary
     */
    @Primary
    @Bean(name = "commandDataSource")
    public DataSource commandDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(commandUrl);
        config.setUsername(commandUsername);
        config.setPassword(commandPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        return new HikariDataSource(config);
    }
    
    /**
     * Query Database (Read Side)
     */
    @Bean(name = "queryDataSource")
    public DataSource queryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(queryUrl);
        config.setUsername(queryUsername);
        config.setPassword(queryPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        return new HikariDataSource(config);
    }
    
    /**
     * Command Entity Manager Factory
     */
    @Primary
    @Bean(name = "commandEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean commandEntityManagerFactory(
            @Qualifier("commandDataSource") DataSource dataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.interexport.guarantees.entity", "com.interexport.guarantees.cqrs.command");
        em.setPersistenceUnitName("command");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        em.setJpaProperties(properties);
        
        return em;
    }
    
    /**
     * Query Entity Manager Factory
     */
    @Bean(name = "queryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean queryEntityManagerFactory(
            @Qualifier("queryDataSource") DataSource dataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.interexport.guarantees.cqrs.query");
        em.setPersistenceUnitName("query");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        em.setJpaProperties(properties);
        
        return em;
    }
    
    /**
     * Command Transaction Manager
     */
    @Primary
    @Bean(name = "commandTransactionManager")
    public PlatformTransactionManager commandTransactionManager(
            @Qualifier("commandEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
    
    /**
     * Query Transaction Manager
     */
    @Bean(name = "queryTransactionManager")
    public PlatformTransactionManager queryTransactionManager(
            @Qualifier("queryEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}

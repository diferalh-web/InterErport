package com.interexport.guarantees;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot Application for the Guarantees Module POC.
 * 
 * This POC implements core functionality for a banking guarantees system
 * based on the technical specifications and requirements documents.
 * 
 * @author InterExport Development Team
 * @version 1.0.0-POC
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableTransactionManagement
public class GuaranteesModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuaranteesModuleApplication.class, args);
    }
}

package com.matthewcasperson.dzonezl;

import org.apache.catalina.filters.ExpiresFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.servlet.Filter;

@SpringBootApplication
@EnableTransactionManagement
public class Application {

    public static void main(String[] args) {
        final ApplicationContext ctx = SpringApplication.run(Application.class, args);

        /*System.out.println("Let's inspect the beans provided by Spring Boot:");

        final String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
            System.out.println(beanName);
        }*/
    }

    @Bean
    public FilterRegistrationBean tomcatExpiresFilter() {
        final FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(expiresFilter());
        registration.addUrlPatterns("/libraries/*");
        registration.addInitParameter("ExpiresByType image", "access plus 1 days");
        registration.addInitParameter("ExpiresByType text/css", "access plus 1 days");
        registration.addInitParameter("ExpiresByType text/css", "access plus 1 days");
        registration.addInitParameter("ExpiresByType text/css", "access plus 1 days");
        registration.addInitParameter("ExpiresByType application/javascript", "access plus 1 days");
        registration.addInitParameter("ExpiresDefault", "access plus 1 days");
        registration.setName("expiresFilter");
        return registration;
    }

    @Bean(name = "expiresFilter")
    public Filter expiresFilter() {
        return new ExpiresFilter();
    }
}
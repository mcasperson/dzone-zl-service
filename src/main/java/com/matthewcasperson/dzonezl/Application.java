package com.matthewcasperson.dzonezl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;


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
}
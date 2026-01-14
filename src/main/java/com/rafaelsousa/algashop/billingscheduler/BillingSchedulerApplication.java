package com.rafaelsousa.algashop.billingscheduler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BillingSchedulerApplication {

	static void main(String[] args) {
		SpringApplication.run(BillingSchedulerApplication.class, args);
	}

}

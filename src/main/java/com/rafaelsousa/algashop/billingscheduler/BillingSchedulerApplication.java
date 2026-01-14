package com.rafaelsousa.algashop.billingscheduler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SpringBootApplication
public class BillingSchedulerApplication {

	static void main(String[] args) {
		SpringApplication.run(BillingSchedulerApplication.class, args);
	}

}

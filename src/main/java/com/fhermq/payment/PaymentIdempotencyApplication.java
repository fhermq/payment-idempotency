package com.fhermq.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentIdempotencyApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentIdempotencyApplication.class, args);
	}

}

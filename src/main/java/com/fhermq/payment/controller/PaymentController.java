package com.fhermq.payment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fhermq.payment.dto.PaymentRequest;
import com.fhermq.payment.dto.PaymentResponse;
import com.fhermq.payment.exception.ErrorCode;
import com.fhermq.payment.exception.IdempotencyException;
import com.fhermq.payment.service.PaymentService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for payment processing endpoints.
 * 
 * Demonstrates best practices for REST API design:
 * - Uses appropriate HTTP methods and status codes
 * - Input validation with @Valid
 * - Proper error handling delegated to GlobalExceptionHandler
 * - Required headers enforced at method level
 * - Clean separation between controller and service logic
 */
@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

	private final PaymentService paymentService;

	@Autowired
	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	/**
	 * Creates and processes a payment with idempotency guarantee.
	 * 
	 * @param idempotencyKey Unique request identifier (required)
	 *                        Must be a valid UUID format
	 *                        Used to guarantee idempotency
	 * @param request Payment request details
	 * @return 201 CREATED with payment details on success
	 * @throws IdempotencyException if payment already processing
	 * @throws Exception Any error is handled by GlobalExceptionHandler
	 * 
	 * Note: All exceptions are handled centrally by GlobalExceptionHandler
	 * which provides consistent error responses across the API.
	 */
	@PostMapping
	public ResponseEntity<PaymentResponse> createPayment(
			@RequestHeader(value = "Idempotency-Key", required = true)
			String idempotencyKey,
			@RequestBody @Valid PaymentRequest request) {

		// Validate idempotency key format
		if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
			throw new IdempotencyException(
				ErrorCode.IDEMPOTENCY_KEY_INVALID,
				"Idempotency key cannot be empty"
			);
		}

		log.info("Processing payment for order: {} with idempotency key: {}",
			request.getOrderId(), idempotencyKey);

		// Service handles all business logic and can throw ApiExceptions
		// which are caught by GlobalExceptionHandler
		PaymentResponse response = paymentService.processPayment(idempotencyKey, request);

		log.info("Payment processed successfully: {}", response.getPaymentId());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
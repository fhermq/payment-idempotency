package com.fhermq.payment.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fhermq.payment.dto.PaymentRequest;
import com.fhermq.payment.dto.PaymentResponse;
import com.fhermq.payment.entity.IdempotencyRecord;
import com.fhermq.payment.entity.Payment;
import com.fhermq.payment.enums.PaymentStatus;
import com.fhermq.payment.enums.RequestStatus;
import com.fhermq.payment.exception.ErrorCode;
import com.fhermq.payment.exception.PaymentException;
import com.fhermq.payment.repository.IdempotencyRepository;
import com.fhermq.payment.repository.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for processing payments with idempotency guarantee.
 * 
 * Demonstrates best practices for service layer design:
 * - Transactional operations for data consistency
 * - Proper exception handling using typed exceptions with error codes
 * - State management for idempotent operations
 * - Clear separation of concerns (payment processing vs idempotency)
 */
@Service
@Slf4j
public class PaymentService {
    
    private final IdempotencyRepository idempotencyRepo;
    private final PaymentRepository paymentRepo;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public PaymentService(
        IdempotencyRepository idempotencyRepo,
        PaymentRepository paymentRepo,
        ObjectMapper objectMapper
    ) {
        this.idempotencyRepo = idempotencyRepo;
        this.paymentRepo = paymentRepo;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Processes a payment with idempotency guarantee.
     * 
     * This method implements the core idempotency pattern:
     * 1. Check if request has been seen before
     * 2. If still processing, return error to retry
     * 3. If completed, return cached response
     * 4. If failed, allow retry
     * 5. If new, process and cache result
     * 
     * @param idempotencyKey Unique identifier for the request
     * @param request Payment request details
     * @return Payment response (cached or newly created)
     * @throws PaymentException For payment processing errors
     */
    @Transactional
    public PaymentResponse processPayment(
        String idempotencyKey, 
        PaymentRequest request
    ) {
        
        log.debug("Processing payment with idempotency key: {}", idempotencyKey);
        
        // Step 1: Check if we've seen this key before
        Optional<IdempotencyRecord> existing = 
            idempotencyRepo.findByIdempotencyKey(idempotencyKey);
        
        if (existing.isPresent()) {
            return handleExistingRequest(existing.get(), idempotencyKey);
        }
        
        // Step 2: Create new idempotency record (PROCESSING state)
        IdempotencyRecord record = createProcessingRecord(idempotencyKey);
        idempotencyRepo.save(record);
        
        try {
            // Step 3: Execute the actual payment
            PaymentResponse response = executePayment(request);
            
            // Step 4: Mark as completed and cache response
            markAsCompleted(record, response);
            
            log.info("Payment {} processed successfully", response.getPaymentId());
            return response;
            
        } catch (PaymentException e) {
            // Expected payment errors - mark as failed
            markAsFailed(record, e.getMessage());
            throw e;
            
        } catch (Exception e) {
            // Unexpected errors - mark as failed
            log.error("Unexpected error processing payment for key {}", 
                idempotencyKey, e);
            markAsFailed(record, e.getMessage());
            
            throw new PaymentException(
                ErrorCode.SYSTEM_INTERNAL_ERROR,
                "Payment processing failed: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Handles requests that have been seen before.
     * Returns appropriate response or error based on previous state.
     */
    private PaymentResponse handleExistingRequest(
            IdempotencyRecord record,
            String idempotencyKey) {
        
        RequestStatus status = record.getStatus();
        
        // Case 1: Request still processing - ask client to retry
        if (status == RequestStatus.PROCESSING) {
            log.info("Request {} still processing", idempotencyKey);
            throw new PaymentException(
                ErrorCode.PAYMENT_ALREADY_PROCESSING,
                "Payment is already being processed for this order. Please retry in a few seconds."
            );
        }
        
        // Case 2: Request completed successfully - return cached response
        if (status == RequestStatus.COMPLETED) {
            log.info("Request {} already completed, returning cached response", 
                idempotencyKey);
            try {
                return objectMapper.readValue(
                    record.getResponse(), 
                    PaymentResponse.class
                );
            } catch (JsonProcessingException e) {
                log.error("Error deserializing cached response for key {}", 
                    idempotencyKey, e);
                throw new PaymentException(
                    ErrorCode.SYSTEM_SERIALIZATION_ERROR,
                    "Failed to deserialize cached response",
                    e
                );
            }
        }
        
        // Case 3: Previous request failed - allow retry
        if (status == RequestStatus.FAILED) {
            log.info("Previous request {} failed, allowing retry", idempotencyKey);
            idempotencyRepo.delete(record);
            // Return null to continue processing - will create new record
            return null;
        }
        
        // This shouldn't happen, but handle defensively
        throw new PaymentException(
            ErrorCode.SYSTEM_INTERNAL_ERROR,
            "Unknown idempotency record state: " + status
        );
    }
    
    /**
     * Creates a new idempotency record in PROCESSING state.
     */
    private IdempotencyRecord createProcessingRecord(String idempotencyKey) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setStatus(RequestStatus.PROCESSING);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusHours(24));
        return record;
    }
    
    /**
     * Marks an idempotency record as completed with cached response.
     */
    private void markAsCompleted(IdempotencyRecord record, PaymentResponse response) {
        try {
            record.setStatus(RequestStatus.COMPLETED);
            record.setResponse(objectMapper.writeValueAsString(response));
            idempotencyRepo.save(record);
        } catch (JsonProcessingException e) {
            log.error("Error serializing payment response", e);
            // Mark as failed if we can't serialize
            record.setStatus(RequestStatus.FAILED);
            record.setResponse(e.getMessage());
            idempotencyRepo.save(record);
            throw new PaymentException(
                ErrorCode.SYSTEM_SERIALIZATION_ERROR,
                "Failed to serialize payment response",
                e
            );
        }
    }
    
    /**
     * Marks an idempotency record as failed.
     */
    private void markAsFailed(IdempotencyRecord record, String errorMessage) {
        record.setStatus(RequestStatus.FAILED);
        record.setResponse(errorMessage);
        idempotencyRepo.save(record);
    }
    
    /**
     * Executes the actual payment processing.
     * This is where the core business logic lives.
     * 
     * @param request Payment request details
     * @return Payment response with transaction details
     * @throws PaymentException For payment-specific errors
     */
    private PaymentResponse executePayment(PaymentRequest request) {
        
        log.info("Processing payment for order {} amount {}", 
            request.getOrderId(), request.getAmount());
        
        // Simulate payment gateway delay
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException(
                ErrorCode.SYSTEM_INTERNAL_ERROR,
                "Payment processing interrupted",
                e
            );
        }
        
        // Create payment record
        Payment payment = Payment.builder()
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .customerId(request.getCustomerId())
            .status(PaymentStatus.SUCCESS)
            .transactionId("TXN-" + UUID.randomUUID().toString())
            .processedAt(LocalDateTime.now())
            .build();
        
        payment = paymentRepo.save(payment);
        
        // Build response
        return PaymentResponse.builder()
            .paymentId(payment.getId().toString())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .status(payment.getStatus())
            .transactionId(payment.getTransactionId())
            .processedAt(payment.getProcessedAt())
            .build();
    }
}
package com.fhermq.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fhermq.payment.dto.PaymentRequest;
import com.fhermq.payment.dto.PaymentResponse;
import com.fhermq.payment.entity.IdempotencyRecord;
import com.fhermq.payment.entity.Payment;
import com.fhermq.payment.enums.RequestStatus;
import com.fhermq.payment.repository.IdempotencyRepository;
import com.fhermq.payment.repository.PaymentRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentIdempotencyTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private PaymentRepository paymentRepo;
    
    @Autowired
    private IdempotencyRepository idempotencyRepo;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    public void setUp() {
        paymentRepo.deleteAll();
        idempotencyRepo.deleteAll();
    }
    
    @Test
    public void testIdempotency_SameKeyTwice_OnlyOnePaymentCreated() throws Exception {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        
        PaymentRequest request = PaymentRequest.builder()
            .orderId("ORD-12345")
            .amount(new BigDecimal("99.99"))
            .currency("USD")
            .customerId("CUST-100")
            .paymentMethod("card")
            .build();
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        // Act - First request
        MvcResult result1 = mockMvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
        .andExpect(status().isCreated())
        .andReturn();
        
        String response1 = result1.getResponse().getContentAsString();
        PaymentResponse paymentResponse1 = 
            objectMapper.readValue(response1, PaymentResponse.class);
        
        // Act - Second request with SAME key
        MvcResult result2 = mockMvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
        .andExpect(status().isCreated())
        .andReturn();
        
        String response2 = result2.getResponse().getContentAsString();
        PaymentResponse paymentResponse2 = 
            objectMapper.readValue(response2, PaymentResponse.class);
        
        // Assert - Same payment ID
        assertEquals(paymentResponse1.getPaymentId(), 
                     paymentResponse2.getPaymentId());
        
        // Assert - Only one payment in database
        List<Payment> payments = paymentRepo.findByOrderId("ORD-12345");
        assertEquals(1, payments.size());
        
        // Assert - Idempotency record exists and is COMPLETED
        IdempotencyRecord record = 
            idempotencyRepo.findByIdempotencyKey(idempotencyKey).orElseThrow();
        assertEquals(RequestStatus.COMPLETED, record.getStatus());
    }
    
    @Test
    public void testIdempotency_DifferentKeys_TwoPaymentsCreated() throws Exception {
        // Arrange
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        
        PaymentRequest request = PaymentRequest.builder()
            .orderId("ORD-12346")
            .amount(new BigDecimal("49.99"))
            .currency("USD")
            .customerId("CUST-101")
            .build();
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        // Act - Request with key1
        mockMvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", key1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
        .andExpect(status().isCreated());
        
        // Act - Request with key2
        mockMvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", key2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
        .andExpect(status().isCreated());
        
        // Assert - Two payments created
        List<Payment> payments = paymentRepo.findByOrderId("ORD-12346");
        assertEquals(2, payments.size());
    }
    
    @Test
    public void testIdempotency_MissingKey_BadRequest() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
            .orderId("ORD-12347")
            .amount(new BigDecimal("29.99"))
            .currency("USD")
            .customerId("CUST-102")
            .build();
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        // Act & Assert - Missing idempotency key
        mockMvc.perform(
            post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
        .andExpect(status().isBadRequest());
    }
}
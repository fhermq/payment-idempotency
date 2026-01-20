package com.fhermq.payment.entity;

import java.time.LocalDateTime;

import com.fhermq.payment.enums.RequestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "idempotency_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

	@Id
	@Column(length = 100)
	private String idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RequestStatus status;

	@Column(columnDefinition = "TEXT")
	private String response;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime expiresAt;
}
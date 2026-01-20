package com.fhermq.payment.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fhermq.payment.entity.IdempotencyRecord;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    
    Optional<IdempotencyRecord> findByIdempotencyKey(String key);
    
    @Modifying
    @Query("DELETE FROM IdempotencyRecord i WHERE i.expiresAt < ?1")
    int deleteByExpiresAtBefore(LocalDateTime cutoff);
}
package com.aspiringinvestments.backend.repository;

import com.aspiringinvestments.backend.entity.InAppPurchase;
import com.aspiringinvestments.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InAppPurchaseRepository extends JpaRepository<InAppPurchase, UUID> {
    
    Optional<InAppPurchase> findByUserAndAppName(User user, String appName);
    
    boolean existsByUserAndAppName(User user, String appName);
}
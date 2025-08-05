package com.aspiringinvestments.backend.service;

import com.aspiringinvestments.backend.entity.InAppPurchase;
import com.aspiringinvestments.backend.entity.User;
import com.aspiringinvestments.backend.repository.InAppPurchaseRepository;
import com.aspiringinvestments.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class InAppPurchaseService {

    @Autowired
    private InAppPurchaseRepository inAppPurchaseRepository;

    @Autowired
    private UserRepository userRepository;

    public InAppPurchase getOrCreatePurchase(UUID userId, String appName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<InAppPurchase> existingPurchase = inAppPurchaseRepository.findByUserAndAppName(user, appName);
        
        if (existingPurchase.isPresent()) {
            return existingPurchase.get();
        } else {
            InAppPurchase newPurchase = new InAppPurchase(user, appName);
            return inAppPurchaseRepository.save(newPurchase);
        }
    }

    @Transactional
    public InAppPurchase updatePurchase(UUID userId, String appName, Integer owned, Integer consumed) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        InAppPurchase purchase = inAppPurchaseRepository.findByUserAndAppName(user, appName)
                .orElseThrow(() -> new RuntimeException("Purchase not found"));

        purchase.setOwned(owned);
        purchase.setConsumed(consumed);
        return inAppPurchaseRepository.save(purchase);
    }

    @Transactional
    public InAppPurchase consumeOne(UUID userId, String appName) {
        InAppPurchase purchase = getOrCreatePurchase(userId, appName);
        
        if (purchase.getRemainingUses() <= 0) {
            throw new RuntimeException("No remaining uses");
        }

        purchase.setConsumed(purchase.getConsumed() + 1);
        
        // If all saves are consumed, reset
        if (purchase.getConsumed() >= purchase.getOwned()) {
            purchase.setOwned(0);
            purchase.setConsumed(0);
        }

        return inAppPurchaseRepository.save(purchase);
    }

    @Transactional
    public InAppPurchase addPurchase(UUID userId, String appName, int additionalOwned) {
        InAppPurchase purchase = getOrCreatePurchase(userId, appName);
        purchase.setOwned(purchase.getOwned() + additionalOwned);
        purchase.setConsumed(0); // Reset consumed when purchasing more
        return inAppPurchaseRepository.save(purchase);
    }

    public int getRemainingUses(UUID userId, String appName) {
        InAppPurchase purchase = getOrCreatePurchase(userId, appName);
        return purchase.getRemainingUses();
    }

    public boolean hasRemainingUses(UUID userId, String appName) {
        return getRemainingUses(userId, appName) > 0;
    }
}
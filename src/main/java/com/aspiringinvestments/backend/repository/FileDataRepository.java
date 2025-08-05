package com.aspiringinvestments.backend.repository;

import com.aspiringinvestments.backend.entity.FileData;
import com.aspiringinvestments.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileDataRepository extends JpaRepository<FileData, UUID> {
    
    Optional<FileData> findByUserAndPath(User user, String path);
    
    List<FileData> findByUserAndPathStartingWith(User user, String pathPrefix);
    
    List<FileData> findByUserOrderByUpdatedAtDesc(User user);
    
    void deleteByUserAndPath(User user, String path);
    
    boolean existsByUserAndPath(User user, String path);
}
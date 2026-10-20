package com.bucketly.api.repository;

import com.bucketly.api.entity.Dream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DreamRepository extends JpaRepository<Dream, Long> {

    List<Dream> findAllByUserId(Long userId);

    Optional<Dream> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}


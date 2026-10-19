package com.bucketly.api.repository;

import com.bucketly.api.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, Long> {

    boolean existsByDreamId(Long dreamId);

    Optional<Memory> findByDreamId(Long dreamId);
}


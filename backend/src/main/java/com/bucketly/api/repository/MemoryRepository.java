package com.bucketly.api.repository;

import com.bucketly.api.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, Long> {

    boolean existsByDreamId(Long dreamId);

    Optional<Memory> findByDreamId(Long dreamId);

    @Query("SELECT m FROM Memory m JOIN FETCH m.dream WHERE m.dream.user.id = :userId")
    List<Memory> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM Memory m JOIN FETCH m.dream WHERE m.id = :id AND m.dream.user.id = :userId")
    Optional<Memory> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Memory m WHERE m.dream.id = :dreamId AND m.dream.user.id = :userId")
    boolean existsByDreamIdAndUserId(@Param("dreamId") Long dreamId, @Param("userId") Long userId);
}


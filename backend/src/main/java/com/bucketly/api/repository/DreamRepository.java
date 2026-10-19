package com.bucketly.api.repository;

import com.bucketly.api.entity.Dream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DreamRepository extends JpaRepository<Dream, Long> {
}


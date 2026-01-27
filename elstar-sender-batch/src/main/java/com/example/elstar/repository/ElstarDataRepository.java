package com.example.elstar.repository;

import com.example.elstar.entity.ElstarData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElstarDataRepository extends JpaRepository<ElstarData, Long> {
    Optional<ElstarData> findByUuid(UUID uuid);
}

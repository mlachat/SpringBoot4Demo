package com.example.elstar.repository;

import com.example.elstar.entity.ElstarData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElstarDataRepository extends JpaRepository<ElstarData, Long> {
    Optional<ElstarData> findByUuid(UUID uuid);

    @Modifying
    @Query("UPDATE ElstarData e SET e.status = :status WHERE e.uuid = :uuid")
    int updateStatusByUuid(@Param("uuid") UUID uuid, @Param("status") Integer status);
}

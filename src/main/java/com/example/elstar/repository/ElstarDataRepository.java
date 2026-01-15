package com.example.elstar.repository;

import com.example.elstar.entity.ElstarData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ElstarDataRepository extends JpaRepository<ElstarData, Long> {
}

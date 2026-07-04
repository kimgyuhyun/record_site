package com.recordsite.backend.repository;

import com.recordsite.backend.entity.ReferenceDataVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenceDataVersionRepository extends JpaRepository<ReferenceDataVersion, String> {
}

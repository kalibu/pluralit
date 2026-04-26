package com.david.pluralit.repository;

import com.david.pluralit.model.entity.ProcessResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessResultRepository extends JpaRepository<ProcessResult, UUID> {
}

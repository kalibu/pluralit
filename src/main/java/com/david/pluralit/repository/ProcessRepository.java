package com.david.pluralit.repository;

import com.david.pluralit.model.entity.Process;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessRepository extends JpaRepository<Process, UUID> {
}

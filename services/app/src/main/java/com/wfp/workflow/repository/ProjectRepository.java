package com.wfp.workflow.repository;

import com.wfp.workflow.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findAllByOrderByKeyAsc();

    boolean existsByKey(String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.key = :key")
    Optional<Project> findByKeyForUpdate(String key);
}

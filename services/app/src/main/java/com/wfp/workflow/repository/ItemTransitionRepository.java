package com.wfp.workflow.repository;

import com.wfp.workflow.entity.ItemTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemTransitionRepository extends JpaRepository<ItemTransition, UUID> {

    List<ItemTransition> findByItemIdOrderByTransitionedAtAsc(UUID itemId);
}

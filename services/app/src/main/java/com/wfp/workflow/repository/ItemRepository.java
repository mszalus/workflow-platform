package com.wfp.workflow.repository;

import com.wfp.workflow.entity.Item;
import com.wfp.workflow.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    Optional<Item> findByKey(String key);
    boolean existsByType(ItemType type);
    List<Item> findByTypeWorkflowKeyAndStatusKeyNot(String workflowKey, String statusKey);
}

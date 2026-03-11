package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsByItemKey(String  itemKey);
}

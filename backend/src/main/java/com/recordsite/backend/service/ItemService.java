package com.recordsite.backend.service;

import com.recordsite.backend.config.CacheConfig;
import com.recordsite.backend.dto.ItemDto;
import com.recordsite.backend.entity.Item;
import com.recordsite.backend.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    // 정적 아이템 목록 — 패치 단위로만 바뀌므로 길게 캐싱(12h).
    @Cacheable(CacheConfig.STATIC_ITEMS)
    public List<ItemDto> findAllItemList() {
        List<ItemDto> itemDtoList = new ArrayList<>();
        List<Item> itemList = itemRepository.findAll();

        for (Item item : itemList) {
            itemDtoList.add(ItemDto.from(item));
        }

        return itemDtoList;
    }
}

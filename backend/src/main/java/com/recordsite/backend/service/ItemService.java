package com.recordsite.backend.service;

import com.recordsite.backend.dto.ItemDto;
import com.recordsite.backend.entity.Item;
import com.recordsite.backend.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public List<ItemDto> findAllItemList() {
        List<ItemDto> itemDtoList = new ArrayList<>();
        List<Item> itemList = itemRepository.findAll();

        for (Item item : itemList) {
            itemDtoList.add(ItemDto.from(item));
        }

        return itemDtoList;
    }
}
